#!/usr/bin/env python
import importers
import sqlite3
import contextlib
import types
import os.path
import matcher
import util
@util.cache
def _fetch_import(importer):
	return importer.do_import()
def _handle_importer(conn, name, importer):
	print 'import', name, 'using', importer
	cached = _fetch_import(importer)
def _run_matcher(conn):
	print 'running matcher'
	for rowid, name, address, phone in conn.execute("SELECT rowid, name, address, phone FROM extractions WHERE business_id IS NULL"):
		business_id = matcher.get_business_id(name=name, address=address, phone=phone)
		conn.execute("UPDATE extractions SET business_id=? WHERE rowid=?", (business_id, rowid))
		if rowid % 1000 == 0:
			conn.commit()
	conn.execute("CREATE UNIQUE INDEX extractions_business_id_index ON extractions(business_id) WHERE business_id NOT NULL")
	conn.commit()
@contextlib.contextmanager
def _get_conn():
	with contextlib.closing(sqlite3.connect('deianeira.db')) as conn:
		yield conn
def main():
	with _get_conn() as conn:
		items = []
		for name, importer in importers.__dict__.iteritems():
			if isinstance(importer, types.ModuleType):
				items.append((name, importer, _fetch_import(importer)))
		conn.execute("DROP TABLE IF EXISTS extractions")
		conn.execute("""
			CREATE TABLE extractions(
				importer TEXT NOT NULL,
				id TEXT NOT NULL,
				name TEXT NOT NULL,
				address TEXT NOT NULL,
				phone TEXT NOT NULL,
				grade TEXT NOT NULL,
				url TEXT,
				data TEXT,
				business_id TEXT,
				PRIMARY KEY(importer ASC, id ASC)
			)
		""")
		for name, importer, data in items:
			for rec in importer.extract(data):
				conn.execute("INSERT INTO extractions VALUES(?, ?, ?, ?, ?, ?, ?, ?)", (
					name,
					rec.id,
					rec.name,
					rec.address,
					rec.phone,
					rec.grade,
					rec.url,
					rec.data,
					None,
				))
		_run_matcher(conn)
if __name__ == '__main__':
	main()
