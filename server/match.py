class Match:
	def __init__(self):
		conns_raw = open("../data_munger/matcher/conn.csv", "r")
		conns_raw.readline()
		conns_dict = {}
		for conn in conns_raw.readlines():
			tokens = conn.split("|")
			conns_dict[tokens[1].strip().decode("utf-8")] = tokens[0].strip().decode("utf-8")
		self.conns = conns_dict
		self.infractions = None

	def get_obj(self, business_id):
		if business_id in self.conns.keys():
			ret = {
				'text': self.conns[business_id], #'o',
				'html': False,
				'color': int("0xff000000", 0)
			}
			return ret
		else:
			ret = {
				'error': "Error, business_id {0} was not found.".format(business_id)
			}
			return ret