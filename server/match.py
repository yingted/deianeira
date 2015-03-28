from blueprint import Blueprint
import rauth

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

class Match:
	def __init__(self):
		with open("../data_munger/matcher/conn.csv", "r") as conns_raw:
			conns_raw.readline()
			conns_dict = {}
			for conn in conns_raw.readlines():
				tokens = [el.strip().decode("utf-8") for el in conn.split("|")]
				conns_dict[tokens[1]] = tokens[0]
			self.conns = conns_dict

		with open("../data_munger/importer/infractions.csv", "r") as infra_raw:
			infra_raw.readline()
			infra_dict = {}
			for infra in infra_raw.readlines():
				tokens = [el.strip().decode("utf-8") for el in infra.split("|")]
				infra_dict[tokens[0]] = tokens[1:]
			self.infractions = infra_dict

	def get_obj(self, raw_id):
		try:
			business_id = get_business_id(raw_id).encode("utf-8").decode("utf-8")
		except Exception, e:
			print "Exception:", e
			business_id = ""

		if business_id in self.conns.keys():
			uuid = self.conns[business_id]
			infras = self.infractions[uuid]
			link = infras[-1]
			text = u'\U0001f604'.encode('utf-8')
			if int(infras[3]) > 0:
				text = u'\U0001f631'.encode('utf-8')
			elif int(infras[4]) > 0:
				text = u'\U0001f628'.encode('utf-8')
			ret = {
				'text': "<a href='{0}'>{1}</a>".format(link,text),
				'html': True,
			}
			return ret
		else:
			text = u'\U0001f610'.encode('utf-8')
			ret = {
				'text': text,
				'html': False,
				'color': 0xff000000
			}
			return ret
