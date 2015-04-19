import joblib
import os.path
import collections
cache = joblib.Memory(cachedir=os.path.join(os.path.dirname(__file__), 'cache')).cache
Record = collections.namedtuple('Record', (
	'id',
	'name',
	'address',
	'phone',
	'grade',
	'url',
	'data',
))
