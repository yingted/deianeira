import cherrypy
import cherrypy_cors
import match
from .. import data_munger

0()

class Server(object):
	def __init__(self):
		object.__init__(self)
		self.matcher = match.Match()

	@cherrypy.expose
	def index(self):
		return "Hello World!"

	@cherrypy.expose
	@cherrypy.tools.json_out(content_type='application/json; charset=utf-8')
	@cherrypy.tools.json_in(force=False)
	def query(self):
		try:
			ids = cherrypy.request.json
		except:
			pass
		print "INPUT:", ids
		ret = []

		for el in ids:
			try:
				ret.append(self.matcher.get_obj(el))
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
	cherrypy.quickstart(Server())
