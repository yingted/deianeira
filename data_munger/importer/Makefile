raw_infractions.csv:
	curl -o $@ 'https://nycopendata.socrata.com/api/views/xx67-kt59/rows.csv?accessType=DOWNLOAD&bom=true'
nyc_infractions.csv: raw_infractions.csv
	./convert_nyc.py < $< > $@
