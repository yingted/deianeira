from .. import util
import mechanize
from BeautifulSoup import BeautifulSoup as bs

@util.cache
def get_records():
	uri = "http://checkit.regionofwaterloo.ca/"
	br = mechanize.Browser()
	br.open(uri + "portal/Facility")
	br.select_form(nr=0)
	br.submit()

	br.select_form(nr=0)
	form = br.form
	form['ProgramArea'] = ['Food']
	br.submit()

	cont = True

	recs = []
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

				raw_infractions = raw_tables[1].findChildren('span')
				non_critical_infractions = 0
				critical_infractions = 0
				for i in range(0,len(raw_infractions),5):
					try:
						# date = raw_infractions[i].getText()
						non_critical_infractions += int(raw_infractions[i+3].getText())
						critical_infractions += int(raw_infractions[i+4].getText())
					except:
						print "INFRACTION ERROR:", uuid

				if critical_infractions > 0:
					grade = "C"
				elif non_critical_infractions > 0:
					grade = "B"
				else:
					grade = "A"

				recs.append(util.Record(
					id=uuid,
					name=title,
					address=address,
					phone=phone_number,
					grade=grade,
					url=uri.rstrip("/") + link,
					data=None,
				))

				br.back()
			except Exception as e:
				print "META ERROR:", uuid, e

		br.follow_link(search_page)
	return recs
