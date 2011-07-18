importPackage( Packages.com.openedit.util );
importPackage( Packages.java.util );
importPackage( Packages.java.lang );
importPackage( Packages.java.io );
importPackage( Packages.com.openedit.modules.update );
importPackage( Packages.com.openedit.modules.scheduler );

var war = "http://dev.entermediasoftware.com/jenkins/job/entermedia-uploader/lastSuccessfulBuild/artifact/deploy/ROOT.war";

var root = moduleManager.getBean("root").getAbsolutePath();
var web = root + "/WEB-INF";
var tmp = web + "/tmp";

log.add("1. GET THE LATEST WAR FILE");
var downloader = new Downloader();
downloader.download( war, tmp + "/ROOT.war");

log.add("2. UNZIP WAR FILE");
var unziper = new ZipUtil();
unziper.unzip(  tmp + "/ROOT.war",  tmp );

log.add("3. REPLACE LIBS");
var files = new FileUtils();

files.deleteMatch( web + "/lib/openedit-db*.jar");
files.copyFileByMatch( tmp + "/WEB-INF/lib/openedit-db*.jar", web + "/lib/");

files.deleteMatch( web + "/lib/antlr*.jar");
files.copyFileByMatch( tmp + "/WEB-INF/lib/antlr*.jar", web + "/lib/");

files.deleteMatch( web + "/lib/asm*.jar");
files.copyFileByMatch( tmp + "/WEB-INF/lib/asm*.jar", web + "/lib/");

files.deleteMatch( web + "/lib/cglib*.jar");
files.copyFileByMatch( tmp + "/WEB-INF/lib/cglib*.jar", web + "/lib/");

files.deleteMatch( web + "/lib/hibernate*.jar");
files.copyFileByMatch( tmp + "/WEB-INF/lib/hibernate*.jar", web + "/lib/");

files.deleteMatch( web + "/lib/mysql-connector*.jar");
files.copyFileByMatch( tmp + "/WEB-INF/lib/mysql-connector*.jar", web + "/lib/");


//log.add("4. UPGRADE BASE DIR");
//files.deleteAll( root + "/base/tracker");
//files.deleteAll( root + "/WEB-INF/base/tracker");
//files.copyFiles( tmp + "/WEB-INF/base/tracker", root + "/WEB-INF/base/tracker");

log.add("5. CLEAN UP");
files.deleteAll(tmp);

log.add("6. DB UPDATE COMPLETED");

