# -*- coding: utf-8 -*-
import cherrypy
import cherrypy_cors
import data_munger.munger as munger
import data_munger.util as util
import traceback
from data_munger.blueprint import Blueprint
import rauth

@util.cache
def get_business_id(business_id):
	keys = Blueprint()
	consumer_key = keys.consumer_key
	consumer_secret = keys.consumer_secret
	token = keys.token
	token_secret = keys.token_secret
	session = rauth.OAuth1Session(consumer_key=consumer_key,consumer_secret=consumer_secret,access_token=token,access_token_secret=token_secret)
	request = session.get("http://api.yelp.com/v2/business/" + business_id ,params={})

	data = request.json()['id'].split("/")[-1]
	session.close()

	return data

def get_obj(raw_id):
	try:
		business_id = get_business_id(raw_id).encode("utf-8").decode("utf-8")
		row = munger.get_row_by_business_id(business_id)
		link = row.url
		if row.grade == 'A':
			text = u'😄'
		elif row.grade == 'C':
			text = u'😱'
		elif row.grade == 'B':
			text = u'😨'
		else:
			text = u'😐'
		ret = {
			'text': "<a href='{0}'>{1}</a>".format(link, text.encode('utf-8')),
			'html': True,
		}
		return ret
	except:
		traceback.print_exc()
	text = u'😐'
	ret = {
		'text': text.encode('utf-8'),
		'html': False,
		'color': 0xff000000
	}
	return ret

class Server(object):

	@cherrypy.expose
	def index(self):
		return "Hello World!"

	@cherrypy.expose
	@cherrypy.tools.json_out(content_type='application/json; charset=utf-8')
	@cherrypy.tools.json_in(force=False)
	def query(self, ids=[]):
		try:
			ids = cherrypy.request.json
		except:
			pass
		print "INPUT:", ids
		ret = []

		for el in ids:
			try:
				ret.append(get_obj(el))
			except Exception as e:
				print("ERROR: ", e)
				ret.append({})
		print "RETURN:", ret
		return ret

def CORS():
	cherrypy.response.headers["Access-Control-Allow-Origin"] = "*"

if __name__ == '__main__':
	cherrypy.tools.CORS = cherrypy.Tool('before_finalize', CORS)
	cherrypy.config.update({'tools.CORS.on': True,})
	cherrypy.server.socket_host = '0.0.0.0'
	cherrypy.server.socket_port = 8081
	munger.install_app()
	cherrypy.quickstart(Server())
