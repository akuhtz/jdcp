/*
 * Copyright (c) 2008 Bradley W. Kimmel
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package ca.eandb.jdcp.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import ca.eandb.util.UnexpectedException;
import ca.eandb.util.args.AbstractCommand;
import ca.eandb.util.io.FileUtil;

/**
 * A <code>Command</code> that reports differences between the classes on the
 * server and the classes in the specified directory tree.
 * @author Brad Kimmel
 */
public final class VerifyCommand extends AbstractCommand<Configuration> {

  /* (non-Javadoc)
   * @see ca.eandb.util.args.AbstractCommand#run(java.lang.String[], java.lang.Object)
   */
  @Override
  protected void run(String[] args, Configuration conf) {
    for (String arg : args) {
      verify("", new File(arg), conf);
    }
  }

  /**
   * Reports classes in the specified directory tree which differ from those
   * on the server or which do not exist on the server.
   * @param pkg The package name associated with the root of the directory
   *     tree.
   * @param path The <code>File</code> indicating the root of the directory
   *     tree.
   * @param conf The application command line options.
   */
  public void verify(String pkg, File path, Configuration conf) {
    if (!path.isDirectory()) {
      throw new IllegalArgumentException(path.getAbsolutePath().concat(" is not a directory."));
    }

    for (File file : path.listFiles()) {
      if (file.isDirectory()) {
        verify(combine(pkg, file.getName()), file, conf);
      } else {
        String fileName = file.getName();
        int extensionSeparator = fileName.lastIndexOf('.');
        if (extensionSeparator >= 0) {
          String extension = fileName.substring(extensionSeparator + 1);
          if (extension.equals("class")) {
            String className = combine(pkg, fileName.substring(0, extensionSeparator));
            try {
              byte[] digest = conf.getJobService().getClassDigest(className);
              if (digest == null) {
                System.out.print("? ");
                System.out.println(className);
              } else if (!matches(file, digest, conf)) {
                System.out.print("* ");
                System.out.println(className);
              } else if (conf.verbose) {
                System.out.print("= ");
                System.out.println(className);
              }
            } catch (FileNotFoundException e) {
              throw new UnexpectedException(e);
            } catch (IOException e) {
              System.out.print("E ");
              System.out.println(className);
            }
          }
        }
      }
    }
  }

  /**
   * Determines whether the digest of the specified file matches the given
   * digest.
   * @param file The <code>File</code> to check.
   * @param digest The digest to compare against.
   * @param conf The application command line options.
   * @return A value indicating whether the digest of the specified file
   *     matches the given digest.
   * @throws IOException If an error occurs while trying to read the file.
   */
  private boolean matches(File file, byte[] digest, Configuration conf) throws IOException {
    byte[] fileDigest = getDigest(file, conf);
    return Arrays.equals(fileDigest, digest);
  }

  /**
   * Gets the digest of the specified class definition.
   * @param def The class definition.
   * @param conf The application command line options.
   * @return The digest of the class definition.
   */
  private byte[] getDigest(File file, Configuration conf) throws IOException {
    byte[] def = FileUtil.getFileContents(file);
    MessageDigest alg;
    try {
      alg = MessageDigest.getInstance(conf.digestAlgorithm);
    } catch (NoSuchAlgorithmException e) {
      throw new UnexpectedException(e);
    }
    return alg.digest(def);
  }

  /**
   * Combines package path.
   * @param parent The parent package.
   * @param child The name of the child package.
   * @return The combined package name.
   */
  private String combine(String parent, String child) {
    if (parent.length() > 0) {
      return parent.concat(".").concat(child);
    } else {
      return child;
    }
  }

}
