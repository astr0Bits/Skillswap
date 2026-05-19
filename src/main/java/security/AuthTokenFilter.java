package security;


import security.jwt.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/*
 * Defines a custom filter that extends OncePerRequestFilter. 
 * It intercepts every request to validate JWT and set authentication in the security context.*/
public class AuthTokenFilter extends OncePerRequestFilter {
	//logger
	private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

	private final JwtUtils jwtUtils;
	private final UserDetailsService userDetailsService;//loading user details by email

	public AuthTokenFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {//Constructor for dependency injection
		this.jwtUtils = jwtUtils;
		this.userDetailsService = userDetailsService;
	}

	//The main filter method. Called for each request.
	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain)
					throws ServletException, IOException {

		String uri = request.getRequestURI();//Gets the requested URI.

		// Skip static and public paths (html, images, css, js, auth endpoints)
		if (uri.matches(".*(\\.css|\\.js|\\.png|\\.jpg|\\.jpeg|\\.gif|\\.svg|\\.ico)$") ||
				uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/") ||
				uri.startsWith("/static/") || uri.startsWith("/webjars/") ||
				uri.startsWith("/api/auth/") || uri.equals("/") || uri.endsWith(".html")) {
			filterChain.doFilter(request, response);
			return;
		}

		logger.info("AuthTokenFilter invoked for request URI: {}", uri);//Logs that the filter is processing the request.

		try {//Extracts the JWT from the Authorization header using parseJwt.
			// Inside doFilterInternal()
			String jwt = parseJwt(request);
			//If the token exists and is valid, extracts the email (subject) from the token using jwtUtils.getEmailFromJwtToken.
			if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
				String email = jwtUtils.getEmailFromJwtToken(jwt);   // changed method name

				UserDetails userDetails = userDetailsService.loadUserByUsername(email);//Loads the user details using the email as the username.

				//Creates an Authentication token with the loaded user details, 
				//no credentials (since they are already verified by the token), and the user’s authorities (roles).
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());

				//Adds request details (like remote address) to the authentication object.
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				//Sets the authentication in the security context. 
				//From this point on, Spring Security treats the user as authenticated.
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		} catch (Exception e) {
			//The filter does not block the request; it just leaves the security context empty (unauthenticated).
			logger.error("Cannot set user authentication: {}", e.getMessage(), e);
		}

		//Continues the filter chain. If authentication was set, 
		//subsequent filters and controllers will see the authenticated user.
		filterChain.doFilter(request, response);
	}

	/*Helper method that extracts the JWT token from the Authorization header. 
	 * It looks for a header starting with Bearer and returns the token part.*/
	private String parseJwt(HttpServletRequest request) {
		String headerAuth = request.getHeader("Authorization");
		if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
			return headerAuth.substring(7);
		}
		return null;
	}
}