/**
 * Description: This class is used to filter the unhidden files
 * @author: Jing Zhao
 **/
package ftp.file;

import java.io.File;
import java.io.FileFilter;

public class UnhideFileFilter implements FileFilter{

	@Override
	public boolean accept(File pathname) {
		if(pathname.isHidden())
			return false;
		else 
			return true;
	}

}
