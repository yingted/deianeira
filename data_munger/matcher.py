#!/usr/bin/python
from blueprint import Blueprint
import rauth
import urllib2
import re
import sys

def search_yelp(params):
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

def get_business_id(name, address, phone):
		phone = ''.join(re.findall('\d+', phone))
		params = {
			'term': name,
			'location':address
		}
		results = search_yelp(params)
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
				if results_phone == phone:
					return business_id
			if results_name == name:
				if results_dist != '':
					if results_dist < 50:
						return business_id
			else:
				return None
		else:
			return None