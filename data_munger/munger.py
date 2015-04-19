#!/usr/bin/env python
import importers
import sqlite3
import contextlib
import types
def _handle_importer(conn, name, importer):
	print 'import', name, 'using', importer
	conn.commit()
def _run_matcher(conn):
	print 'running matcher'
def main():
	with contextlib.closing(sqlite3.connect('deianeira.db')) as conn:
		for name, importer in importers.__dict__.iteritems():
			if isinstance(importer, types.ModuleType):
				_handle_importer(conn, name, importer)
		_run_matcher(conn)
if __name__ == '__main__':
	main()
