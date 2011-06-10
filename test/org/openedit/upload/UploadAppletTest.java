package org.openedit.upload;

import org.openedit.sync.Sync;
import org.openedit.uploader.HttpUploader;
import org.openedit.uploader.Upload;

import com.openedit.BaseTestCase;

public class UploadAppletTest extends BaseTestCase
{

	public void testSync() throws Exception {
		//run archive before running this test.
		
		//Must run the CreateUploadAssetTest
		
		
		Sync sync = new Sync();
		
		sync.setAppletName("1234");
		
		String webapp = "";
		
		String url =  "http://localhost:8080" + webapp + "/entermedia/upload/sync/savefile.html";
		sync.setUploadUrl(url);

		sync.setListUrl("http://localhost:8080" + webapp + "/entermedia/upload/sync/pendingfiles.xml");
		                                                             
		String cookie = "openedit.usermanager.autologin" + webapp + "Un1qu3_str1ng=adminOEWITHOEDESl/3Zgvj7X4qSK5bEkjPrrXs0tD6Rn47n";
		sync.setCookies(cookie);

		String pending = sync.getPending();
		assertNotNull(pending);
		assertTrue(pending.contains("test1.jpg"));
		
		sync.uploadFiles();
		pending = sync.getPending();
		if( pending != null)
		{
			assertFalse(pending.contains("test1.jpg"));
		}
		
	}

	public void xxtestUploadPNG() throws Exception{
		//run archive before running this test.
		HttpUploader upload = new HttpUploader();
		Upload target = new Upload();
		String cookie = "openedit.usermanager.autologin/archivesUn1qu3_str1ng=adminOEWITHOEDESl/3Zgvj7X4qSK5bEkjPrrXs0tD6Rn47n; JSESSIONID=abcxTLqK6FWbd7tcRAt-r";
		String url =  "http://localhost:8080/archives/openedit/files/upload/uploadfile-finish.html";
		target.setCookies(cookie);
		target.setLocalPath("/home/ian/Desktop/demo.png");
		target.setServerPostURL(url);
		
		upload.send(target);

	}
	
}
