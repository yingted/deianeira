#!/usr/bin/env python
import csv
import sys
import codecs
import pandas as pd
import pandas.io.parsers
import datetime
import numpy as np
class pipe_separated(csv.excel):
	delimiter = '|'
def _parse_dates(dates):
	return np.fromiter((datetime.datetime.strptime(date, '%m/%d/%Y') for date in dates), dtype='datetime64[us]')
def do_import(infile, outfile):
	df = pd.io.parsers.read_csv(infile, converters={
		'STREET': str.strip,
	})
	df = df[~df.GRADE.isnull()]
	df = df[df['GRADE DATE'] <= df['GRADE DATE']]
	df['GRADE DATE'] = _parse_dates(df['GRADE DATE'])
	df.set_index('GRADE DATE', inplace=True)
	df.sort_index(inplace=True)
	df.drop_duplicates('CAMIS', take_last=True, inplace=True)
	df.reset_index(inplace=True)
	df['address'] = df[['STREET', 'BORO', 'ZIPCODE']].apply('%(STREET)s %(BORO)s %(ZIPCODE)05.f'.__mod__, axis=1, raw=False)

	out = csv.writer(outfile, dialect=pipe_separated)
	out.writerow(('uuid', 'title', 'address', 'phone_number', 'grade'))
	for i, row in df.iterrows():
		try:
			phone = '%.f' % float(row['PHONE'])
		except ValueError:
			phone = ''
		out.writerow((
			'nyc:%s' % row['CAMIS'],
			row['DBA'],
			row['address'],
			phone,
			row['GRADE']
		))
def main(inpath=None, outpath=None):
	assert (inpath is None) == (outpath is None)
	reader = codecs.getreader('utf-8-sig')
	writer = codecs.getreader('utf-8')
	reload(sys)
	sys.setdefaultencoding('UTF-8')
	sys.stdin = reader(sys.stdin)
	sys.stdout = writer(sys.stdout)
	if inpath is None:
		do_import(sys.stdin, sys.stdout)
	else:
		with open(inpath, 'rb') as infile, open(outpath, 'rb') as outfile:
			do_import(reader(infile), writer(outfile))
if __name__ == '__main__':
	main(*sys.argv[1:])
