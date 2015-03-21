import cherrypy
import cherrypy_cors
import match

class Server(object):
	def __init__(self):
		object.__init__(self)
		self.matcher = match.Match()
		print "CONNS:", self.matcher.conns

	@cherrypy.expose
	def index(self):
		return "Hello World!"

	@cherrypy.expose
	#@cherrypy_cors.tools.expose()
	@cherrypy.tools.json_out()
	@cherrypy.tools.json_in(force=False)
	def query(self, ids=[]):
		print "IDs:", ids
		ret = []

		for el in ids:
			try:
				ret.append(self.matcher.get_obj(el))
			except Exception as e:
				print("ERROR: ", e)
				ret.append({})
		return ret

def CORS():
	cherrypy.response.headers["Access-Control-Allow-Origin"] = "*"


if __name__ == '__main__':
	cherrypy.tools.CORS = cherrypy.Tool('before_finalize', CORS)
	cherrypy.config.update({'tools.CORS.on': True,})
	cherrypy.server.socket_host = '0.0.0.0'
	cherrypy.quickstart(Server())
