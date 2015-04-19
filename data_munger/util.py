import joblib
import collections
cache = joblib.Memory(cachedir=os.path.join(os.path.dirname(__file__), 'cache')).cache
Record = collections.namedtuple((
	'id',
	'name',
	'address',
	'phone',
	'grade',
	'phone',
	'url',
	'data',
))
