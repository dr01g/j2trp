package com.j2trp.core.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.j2trp.core.ModifiableHttpRequest;

public class TestFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		ModifiableHttpRequest modReq = new ModifiableHttpRequest((HttpServletRequest) request);
		modReq.addHeader("X-Auth-User", "Alice");
		chain.doFilter(modReq, response);
	}

	@Override
	public void destroy() {

	}

}
