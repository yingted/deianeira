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
	green = int("0x00ff00ff", 0)
	yellow = int("0xffff00ff", 0)
	red = int("0xff0000ff", 0)

	def __init__(self):
		conns_raw = open("../data_munger/matcher/conn.csv", "r")
		conns_raw.readline()
		conns_dict = {}
		for conn in conns_raw.readlines():
			tokens = [el.strip().decode("utf-8") for el in conn.split("|")]
			conns_dict[tokens[1]] = tokens[0]
		self.conns = conns_dict

		infra_raw = open("../data_munger/importer/infractions.csv", "r")
		infra_raw.readline()
		infra_dict = {}
		for infra in infra_raw.readlines():
			tokens = [el.strip().decode("utf-8") for el in infra.split("|")]
			infra_dict[tokens[0]] = tokens[1:]
		self.infractions = infra_dict

	def get_obj(self, raw_id):
		business_id = get_business_id(raw_id).decode("utf-8")
		if business_id in self.conns.keys():
			uuid = self.conns[business_id]
			infras = self.infractions[uuid]
			color = self.green
			if int(infras[-1]) > 0:
				color = self.red
			elif int(infras[-2]) > 0:
				color = self.yellow
			ret = {
				'text': 'o',
				'html': False,
				'color': color
			}
			return ret
		else:
			ret = {
				'test': '',
				'html': False,
				'color': self.green
			}
			return ret
