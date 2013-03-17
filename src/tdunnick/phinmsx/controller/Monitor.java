/*
 *  Copyright (c) 2012-2013 Thomas Dunnick (https://mywebspace.wisc.edu/tdunnick/web)
 *  
 *  This file is part of PhinmsX.
 *
 *  PhinmsX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  PhinmsX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with PhinmsX.  If not, see <http://www.gnu.org/licenses/>.
 */

package tdunnick.phinmsx.controller;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.logging.*;
import tdunnick.phinmsx.domain.*;
import tdunnick.phinmsx.model.*;
import tdunnick.phinmsx.util.*;

/**
 * Servlet implementation class MonitorServlet
 */
public class Monitor extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	public final static String Version = "Monitor RO 0.10 08/08/2012";
	private static Props props = null;
	private static MonitorModel mon = null;
	private Logger logger = null;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Monitor()
	{
		super();
	}
	
	/**
	 * Retrieve a default serverlet property.  Note, most properties should
	 * come from the request environment which includes these...
	 * 
	 * @param name of property sans prefix
	 * @return value or null if not found
	 */
	private String getProperty (String name)
	{
		if (props == null)
			return ("");
		return props.getProperty (name);
	}


	/**
	 * do basic initialization needed for this servlet
	 * 
	 * @param conf
	 *          name of XML properties files
	 * @return true if successful
	 */
	private boolean initialize(String conf)
	{
		boolean ok;
		props = new Props();
		if (!props.load(conf))
		{
			logger = Logger.getLogger("");
			logger.severe("Failed initializing " + conf);
			props = null;
			return false;
		}
		logger = props.getLogger ();
		logger.info ("Started PhinmsX Monitor");
		mon = new MonitorModel ();
		ok = mon.initialize(props);
		return (ok);
	}

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException
	{
		super.init();
		logger = Logger.getLogger("");
		logger.info ("Initializing Monitor Servlet ...");
		PhinmsX.setContextPath(config.getServletContext().getRealPath("/"));
		if (initialize(config.getInitParameter("Config")))
			logger.info ("Monitor running against " + Phinms.getVersion());
	}

	/**
	 * @see Servlet#destroy()
	 */
	public void destroy()
	{
		logger.info ("Exiting...");
		props.close ();
		mon.close ();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException
	{
		doPost (request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException
	{
		String s = request.getServletPath();
		String f = null;

		logger.finest ("request path=" + s);
		Object o = null;
		if (s.indexOf ("dashboard.html") >= 0)
		{
			if ((o = mon.getDashBoardData (request)) != null)
			{
				request.setAttribute("dashboard", o);
				f = "dashboard.jsp";
			}
		}
		else if (s.indexOf ("queues.html") >= 0)
		{
			if ((o = mon.getMonitorData(request)) != null)
			{
				request.setAttribute("queues", o);
				f = "queues.jsp";
			}
		}
		else if (s.indexOf (".png") >= 0)
		{
			byte[] img = mon.getChart(s);
			if (img != null)
			{
				logger.finest ("img size=" + img.length);
				response.setHeader("Content-Type", "image/png");
				response.setHeader("Content-Length", Integer.toString (img.length));
				OutputStream out = response.getOutputStream();
				out.write(img);
				out.close();
				return;
			}
		}
		else
		{
			f = "index.html";
		}
		if (f == null)
		{
			request.setAttribute("error", new ErrorData (props));
			f = "error.jsp";
		}
		RequestDispatcher view = request.getRequestDispatcher(f);
		view.forward(request, response);
	}
}
