## Admin Username/Password

## Additional Features
(1) Messaging, (2) Favorites, (3) Deleting Properties and Users, (4) Browse Property Filters

## Contribution Summary
- config
  - CustomAccessDeniedHandler.java
  - CustomAuthenticationEntryPoint.java
  - SecurityConfig.java
- controller
  - UserController.java: All
- dtos
  - ApiExceptionDto.java
  - JwtResponse.java
  - LoginRequest.java
- entities
  - Favorite.java
  - Property.java
  - PropertyImage.java
  - User.java: Madeleine (support)
- exceptions
  - NotFoundException.java: Madeleine
  - UserAlreadyExistsException.java: Madeleine
- initializer
  - DataInitializer.java
- jwt
  - JwtUtil.java
- repository
  - FavoriteRepository.java
  - PropertyImageRepository.java
  - PropertyRepository.java
  - UserRepository.java
- service
  - AuthService.java
  - AuthServiceImpl.java
  - CustomUserDetailsService.java
  - CustomUserDetailsServiceImpl.java
  - UserService.java: All
  - UserServiceImpl.java: All
- utils
  - CurrentUserContext.java
  - GlobalRateLimiterFilter.java
  - JwtAuthenticationFilter.java
  - JwtSecretGenerator.java

- css
  - admin_style.css: Madeleine
  - dashboard_style.css: Madeleine (support)
  - login.css
  - profile_style.css
  - property_style.css
- templates
  - add_agent.html: Madeleine
  - add_properties.html
  - all_users.html: Madeleine
  - browse_properties.html
  - dashboard.html: All
  - edit_profile.html
  - favorites.html
  - fragments.html
  - index.html
  - login.html
  - manage_properties.html
  - messages.html
  - my_profile.html
  - property_view.html
  - register.html
