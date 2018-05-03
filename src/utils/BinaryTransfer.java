
package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BinaryTransfer {

	private BinaryTransfer() {

	}

	public static void toStream(OutputStream os, File file) {

		int length = 0;
		int realLength = 0;
		byte[] buffer = new byte[1024];

		try {
			FileInputStream fis = new FileInputStream(file);
			int totalLength = fis.available();

			buffer[0] = (byte) (totalLength & 0xFF);
			totalLength >>= 8;
			buffer[1] = (byte) (totalLength & 0xFF);
			totalLength >>= 8;
			buffer[2] = (byte) (totalLength & 0xFF);
			totalLength >>= 8;
			buffer[3] = (byte) (totalLength & 0xFF);

			os.write(buffer , 0, 4);

			while ( ( length = fis.read(buffer) ) > 0 ) {
				os.write(buffer, 0, length);
				realLength += length;
			}

			assert( realLength == totalLength );

			os.flush();
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void toFile(InputStream in, File file) {

		int length = 0;
		int realLength = 0;
		byte[] buffer = new byte[1024];

		try {
			FileOutputStream fos = new FileOutputStream(file);
			int totalLength = 0;

			in.read(buffer , 0, 4);

			totalLength += buffer[3];
			totalLength <<= 8;
			totalLength += buffer[2];
			totalLength <<= 8;
			totalLength += buffer[1];
			totalLength <<= 8;
			totalLength += buffer[0];

			while ( ( length = in.read(buffer) ) > 0 ) {
				fos.write(buffer, 0, length);
				realLength += length;
				if ( realLength == totalLength ) break;
			}
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
