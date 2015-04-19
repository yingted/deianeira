conn.csv: conn-nyc.csv conn-waterloo.csv
	cat $* > $@

conn-%.csv: %-import.csv
	./matcher.py $< $@