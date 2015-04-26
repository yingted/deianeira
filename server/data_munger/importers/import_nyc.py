from .. import util
import urllib2
import contextlib
import codecs
import pandas as pd
import pandas.io.parsers
import datetime
import numpy as np
import cStringIO as StringIO
import cgi
@util.cache
def fetch_raw():
	with contextlib.closing(urllib2.urlopen('https://nycopendata.socrata.com/api/views/xx67-kt59/rows.csv?accessType=DOWNLOAD&bom=true')) as f:
		return f.read()
def _parse_dates(dates):
	return np.fromiter((datetime.datetime.strptime(date, '%m/%d/%Y') for date in dates), dtype='datetime64[us]')
def get_records():
	with contextlib.closing(StringIO.StringIO(fetch_raw())) as in_f:
		with codecs.getreader('utf-8-sig')(in_f) as inp:
			df = pd.io.parsers.read_csv(inp, converters={
				'STREET': str.strip,
			})
	df_by_camis = df.set_index('CAMIS')
	df = df[~df.GRADE.isnull()]
	df = df[df['GRADE DATE'] <= df['GRADE DATE']]
	df['GRADE DATE'] = _parse_dates(df['GRADE DATE'])
	df.set_index('GRADE DATE', inplace=True)
	df.sort_index(inplace=True)
	df.drop_duplicates('CAMIS', take_last=True, inplace=True)
	df.reset_index(inplace=True)
	df['address'] = df[['STREET', 'BORO', 'ZIPCODE']].apply('%(STREET)s %(BORO)s %(ZIPCODE)05.f'.__mod__, axis=1, raw=False)
	for i, row in df.iterrows():
		try:
			phone = '%.f' % float(row['PHONE'])
		except ValueError:
			phone = ''
		yield util.Record(
			id=row['CAMIS'],
			name=row['DBA'],
			address=row['address'],
			phone=phone,
			grade=row['GRADE'],
			url=None,
			data=df_by_camis.loc[row['CAMIS']].to_dict(),
		)
def render(row):
	df = pd.DataFrame.from_dict(row.data)
	df_html = df.to_html()
	title_html = '%s (%s)' % (cgi.escape(row.name), cgi.escape(row.address))
	return '''\
<!DOCTYPE html>
<html>
	<head>
		<title>%(title_html)s</title>
	</head>
	<body>
		<h1>%(title_html)s</h1>
		%(df_html)s
	</body>
</html>
''' % locals()
