mvn install:install-file \
	-Dfile=/Users/appfirst/git/statsd_clients/java/lib/jna-3.2.7.jar \
	-DgroupId=net.java.dev.jna \
	-DartifactId=jna \
	-Dversion=3.2.7 \
	-Dpackaging=jar \
	-DlocalRepositoryPath=/Users/appfirst/git/statsd_clients/java/lib/repo
mvn install:install-file \
	-Dfile=/Users/appfirst/git/statsd_clients/java/lib/platform-3.2.7.jar \
	-DgroupId=net.java.dev.jna \
	-DartifactId=platform \
	-Dversion=3.2.7 \
	-Dpackaging=jar \
	-DlocalRepositoryPath=./lib/repo