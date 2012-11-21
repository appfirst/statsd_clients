mvn install:install-file \
	-Dfile=./lib/jna-3.2.7.jar \
	-DgroupId=net.java.dev.jna \
	-DartifactId=jna \
	-Dversion=3.2.7 \
	-Dpackaging=jar \
	-DlocalRepositoryPath=./lib/repo
mvn install:install-file \
	-Dfile=./lib/platform-3.2.7.jar \
	-DgroupId=net.java.dev.jna \
	-DartifactId=platform \
	-Dversion=3.2.7 \
	-Dpackaging=jar \
	-DlocalRepositoryPath=./lib/repo
