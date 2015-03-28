import sys
import mechanize
from BeautifulSoup import BeautifulSoup as bs

uri = "http://checkit.regionofwaterloo.ca/"

meta = open("meta.csv", "w")
meta.write("uuid|title|address|phone_number\n")
infractions = open("infractions.csv", "w")
infractions.write("uuid|date|inspection_type|certified_food_handler_on_site|non_critical_infractions|critical_infractions|url\n")

br = mechanize.Browser()
br.open(uri + "portal/Facility")
br.select_form(nr=0)
br.submit()

br.select_form(nr=0)
form = br.form
form['ProgramArea'] = ['Food']
br.submit()

cont = True

while(cont):
	links = list(br.links())
	if (links[-1].text != "Next"):
		cont = False

	info_pages = []
	search_page = links[-1]
	print "DEBUG:", search_page.url

	for link in links:
		if link.text == "Details":
			info_pages.append(link)

	for page in info_pages:
		link = page.url
		uuid = page.url.split("/")[-1]
		try:
			raw_page = br.follow_link(page)
			raw_soup = bs(raw_page, convertEntities=bs.HTML_ENTITIES)
			raw_tables = raw_soup.findAll('table')

			raw_meta = raw_tables[0].findChildren('td')
			address = raw_meta[1].getText().split(" - ")[-1]
			phone_number = raw_meta[-1].getText()
			title = raw_soup.findAll('h1')[0].getText()
			meta.write("{0}|{1}|{2}|{3}\n".format(
				uuid,
				title,
				address,
				phone_number
			))

			raw_infractions = raw_tables[1].findChildren('span')

			for i in range(0,len(raw_infractions),5):
				try:
					date = raw_infractions[i].getText()
					inspection_type = raw_infractions[i+1].getText()
					certified_food_handler_on_site = raw_infractions[i+2].getText()
					non_critical_infractions = raw_infractions[i+3].getText()
					critical_infractions = raw_infractions[i+4].getText()
					infractions.write("{0}|{1}|{2}|{3}|{4}|{5}|{6}\n".format(
						uuid,
						date,
						inspection_type,
						certified_food_handler_on_site,
						non_critical_infractions,
						critical_infractions,
						uri.rstrip("/") + link
					))
				except:
					print "INFRACTION ERROR:", uuid

			br.back()
		except Exception as e:
			print "META ERROR:", uuid, e

	br.follow_link(search_page)


meta.close()
infractions.close()