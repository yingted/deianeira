import util
@util.cache
def extract():
	pass
def get_records():
	data = extract()
	yield util.Record(
		id='',
		name='',
		address='',
		phone='',
		grade='',
		url=None,
		data=None,
	)
