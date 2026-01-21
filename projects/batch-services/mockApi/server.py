import json,os
from flask import Flask
from flask import Response
from flask import request


app = Flask(__name__)

@app.route('/students', methods = ['GET'])
def getStudents():
    return Response(json.dumps([{"id":"111111","firstName":"John","lastName":"Doe","email":"jdoe@example.com"},{"id":"111112","firstName":"Jane","lastName":"Smith","email":"jsmith@example.com"},{"id":"111113","firstName":"Sarah","lastName":"Thomas","email":"sthomas@example.com"},{"id":"111114","firstName":"Frank","lastName":"Brown","email":"fbrown@example.com"},{"id":"111115","firstName":"Mike","lastName":"Davis","email":"mdavis@example.com"},{"id":"111116","firstName":"Jennifer","lastName":"Wilson","email":"jwilson@example.com"},{"id":"111117","firstName":"Jessica","lastName":"Garcia","email":"jgarcia@example.com"},{"id":"111118","firstName":"Fred","lastName":"Clark","email":"fclark@example.com"},{"id":"111119","firstName":"Bob","lastName":"Lopez","email":"blopez@example.com"}]), mimetype='application/json')

@app.route('/students', methods = ['POST'])
def addStudents():
	print("added to database")
	print(request.form)
	return Response(json.dumps(request.form), mimetype='application/json')


app.run(host=os.getenv('IP', '0.0.0.0'),port=int(os.getenv('PORT', 8001)))