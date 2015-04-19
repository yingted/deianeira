#!/usr/bin/env python
import importers
import sqlite3
import contextlib
import types
import matcher
import util
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
		for name, importer in importers.__dict__.iteritems():
			if isinstance(importer, types.ModuleType):
				for rec in importer.get_records():
					conn.execute("INSERT INTO extractions VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)", (
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
