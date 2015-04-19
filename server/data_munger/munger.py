#!/usr/bin/env python
import importers
import sqlite3
import contextlib
import types
import matcher
import util
import cherrypy
import urllib
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
		conn.text_factory = str
		yield conn
def dict_factory(cursor, row): # from docs
    d = {}
    for idx, col in enumerate(cursor.description):
        d[col[0]] = row[idx]
    return d
@contextlib.contextmanager
def _get_row_conn():
	with _get_conn() as conn:
		old_row_factory = conn.row_factory
		conn.row_factory = dict_factory
		yield conn
		conn.row_factory = old_row_factory
def _importer_items():
	for name, importer in importers.__dict__.iteritems():
		if isinstance(importer, types.ModuleType):
			yield name, importer
_importer_dict = dict(_importer_items())
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
		for name, importer in _importer_items():
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
			conn.commit()
		_run_matcher(conn)
class Renderer(object):
	@cherrypy.expose
	@cherrypy.tools.response_headers(headers=[('Content-Type', 'text/html')])
	def index(self, kind, id):
		if kind not in _importer_dict:
			raise cherrypy.NotFound()
		importer = _importer_dict[kind]
		with _get_row_conn() as conn:
			for row in conn.execute("SELECT * FROM extractions WHERE importer=? AND id=? LIMIT 1", (kind, id)):
				return importer.render(row)
			else:
				raise cherrypy.NotFound()
def install_app():
	cherrypy.tree.mount(Renderer(), '/render')
def get_row_by_business_id(business_id):
	with _get_row_conn() as conn:
		for row in conn.execute("SELECT * FROM extractions WHERE business_id=?", (business_id,)):
			if row['url'] is None:
				row['url'] = cherrypy.url('/render/', qs=urllib.urlencode({
					'kind': row['importer'],
					'id': row['id'],
				}))
				print 'made url', row['url'], 'for', cherrypy.request.base
			return row
		else:
			raise KeyError(business_id)
if __name__ == '__main__':
	main()
