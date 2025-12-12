echo "Running Liquibase"
dbServerName=$1
dbUserName=$2
dbPassword=$3
dbName=workmanagement

java -jar camunda-liquibase.jar --url=jdbc:postgresql://${dbServerName}:5432/${dbName}?sslmode=require --username=${dbUserName} --password=${dbPassword} --logLevel=info update
if [ $? -ne 0 ]
then
    exit 1
else
    echo success!
fi