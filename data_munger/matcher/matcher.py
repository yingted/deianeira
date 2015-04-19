#!/usr/bin/python
from blueprint import Blueprint
import rauth
import urllib2
import re
import sys

def search(params):
	keys = Blueprint()
	consumer_key = keys.consumer_key
	consumer_secret = keys.consumer_secret
	token = keys.token
	token_secret = keys.token_secret

	session = rauth.OAuth1Session(consumer_key=consumer_key,consumer_secret=consumer_secret,access_token=token,access_token_secret=token_secret)

	request = session.get("http://api.yelp.com/v2/search",params=params)

	data = request.json()
	session.close()

	return data

def do_match(locations, connections):
	locations.readline()
	locations = locations.readlines()
	connections.write("uuid|business_id\n")

	for location in locations:
		tokens = location.split("|")
		uuid = tokens[0]
		name = tokens[1].lower()
		address = tokens[2].lower()
		phone = tokens[3]
		phone = re.findall('\d+', phone)
		phone = ''.join(phone)
		params = {
			'term': name,
			'location':address
		}
		results = search(params)
		if 'businesses' in results.keys() and (len(results['businesses']) > 0):
			business_id = results['businesses'][0]['id'].encode("utf-8")
			results_phone = ''
			results_name = ''
			results_dist = ''
			try:
				results_phone = results['businesses'][0]['phone']
			except:
				pass
			try:
				results_dist = float(results['businesses'][0]['distance'])
			except:
				pass

			results_name = results['businesses'][0]['name'].encode("utf-8")

			if results_phone != '':
				if phone == results_phone:
					# print "SUCCESS: MATCH FOUND | ", uuid, business_id
					connections.write("{0}|{1}\n".format(uuid, business_id))
					continue
			if name == results_name:
				if results_dist != '':
					if results_dist < 50:
						# print "SUCCESS: MATCH FOUND | ", uuid, business_id
						connections.write("{0}|{1}\n".format(uuid, business_id))
						continues
			else:
				print "ERROR: NOT MATCHING |", name, phone + " | " + results_name, results_phone, results_dist
		else:
			print "ERROR: NOT FOUND |", name, address, phone

def main():
	if len(sys.argv) == 1:
		do_match(sys.stdin, sys.stdout)
		return
	locations_dir = sys.argv[1]
	conns_dir = sys.argv[2]
	with open(locations_dir, "r") as locations, open(conns_dir, "w") as connections:
		do_match(locations, connections)

if __name__ == '__main__':
	main()