/*
 *  soapUI, copyright (C) 2004-2012 smartbear.com 
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the 
 *  terms of version 2.1 of the GNU Lesser General Public License as published by 
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */

package com.eviware.soapui.support.jnlp;

import java.io.File;

public class WebstartUtilCore extends WebstartUtil
{

	public static void init()
	{
		if( isWebStart() )
		{
			try
			{
				// if( System.getProperty( "deployment.user.tmp" ) != null
				// && System.getProperty( "deployment.user.tmp" ).length() > 0 )
				// {
				// System.setProperty( "GRE_HOME", System.getProperty(
				// "deployment.user.tmp" ) );
				// }

				// wsi-test-tools
				System.setProperty( "wsi.dir",
						createWebStartDirectory( "wsi-test-tools", System.getProperty( "wsitesttools.jar.url" ) )
								+ File.separator + "wsi-test-tools" );
				System.out.println( System.getProperty( "wsi.dir" ) );
			}
			catch( Exception e )
			{
				e.printStackTrace();
			}

		}

	}
}
