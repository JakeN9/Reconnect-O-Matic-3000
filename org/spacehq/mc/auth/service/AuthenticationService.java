package org.spacehq.mc.auth.service;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.spacehq.mc.auth.data.GameProfile;
import org.spacehq.mc.auth.data.GameProfile.Property;
import org.spacehq.mc.auth.exception.request.InvalidCredentialsException;
import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.auth.util.HTTP;

public class AuthenticationService
{
  private static final String BASE_URL = "https://authserver.mojang.com/";
  private static final String AUTHENTICATE_URL = "https://authserver.mojang.com/authenticate";
  private static final String REFRESH_URL = "https://authserver.mojang.com/refresh";
  private static final String INVALIDATE_URL = "https://authserver.mojang.com/invalidate";
  private String clientToken;
  private Proxy proxy;
  private String username;
  private String password;
  private String accessToken;
  private boolean loggedIn;
  private String id;
  private List<GameProfile.Property> properties = new ArrayList();
  private List<GameProfile> profiles = new ArrayList();
  private GameProfile selectedProfile;
  
  public AuthenticationService()
  {
    this(UUID.randomUUID().toString());
  }
  
  public AuthenticationService(Proxy proxy)
  {
    this(UUID.randomUUID().toString(), proxy);
  }
  
  public AuthenticationService(String clientToken)
  {
    this(clientToken, Proxy.NO_PROXY);
  }
  
  public AuthenticationService(String clientToken, Proxy proxy)
  {
    if (clientToken == null) {
      throw new IllegalArgumentException("ClientToken cannot be null.");
    }
    if (proxy == null) {
      throw new IllegalArgumentException("Proxy cannot be null.");
    }
    this.clientToken = clientToken;
    this.proxy = proxy;
  }
  
  public String getClientToken()
  {
    return this.clientToken;
  }
  
  public String getUsername()
  {
    return this.id;
  }
  
  public String getPassword()
  {
    return this.password;
  }
  
  public String getAccessToken()
  {
    return this.accessToken;
  }
  
  public boolean isLoggedIn()
  {
    return this.loggedIn;
  }
  
  public String getId()
  {
    return this.id;
  }
  
  public List<GameProfile.Property> getProperties()
  {
    return isLoggedIn() ? new ArrayList(this.properties) : Collections.emptyList();
  }
  
  public List<GameProfile> getAvailableProfiles()
  {
    return this.profiles;
  }
  
  public GameProfile getSelectedProfile()
  {
    return this.selectedProfile;
  }
  
  public void setUsername(String username)
  {
    if ((this.loggedIn) && (this.selectedProfile != null)) {
      throw new IllegalStateException("Cannot change username while user is logged in and profile is selected.");
    }
    this.username = username;
  }
  
  public void setPassword(String password)
  {
    if ((this.loggedIn) && (this.selectedProfile != null)) {
      throw new IllegalStateException("Cannot change password while user is logged in and profile is selected.");
    }
    this.password = password;
  }
  
  public void setAccessToken(String accessToken)
  {
    if ((this.loggedIn) && (this.selectedProfile != null)) {
      throw new IllegalStateException("Cannot change access token while user is logged in and profile is selected.");
    }
    this.accessToken = accessToken;
  }
  
  public void login()
    throws RequestException
  {
    if ((this.username == null) || (this.username.equals(""))) {
      throw new InvalidCredentialsException("Invalid username.");
    }
    if ((this.accessToken != null) && (!this.accessToken.equals("")))
    {
      loginWithToken();
    }
    else
    {
      if ((this.password == null) || (this.password.equals(""))) {
        throw new InvalidCredentialsException("Invalid password.");
      }
      loginWithPassword();
    }
  }
  
  public void logout()
    throws RequestException
  {
    if (!this.loggedIn) {
      throw new IllegalStateException("Cannot log out while not logged in.");
    }
    InvalidateRequest request = new InvalidateRequest(this.clientToken, this.accessToken);
    HTTP.makeRequest(this.proxy, "https://authserver.mojang.com/invalidate", request);
    
    this.accessToken = null;
    this.loggedIn = false;
    this.id = null;
    this.properties.clear();
    this.profiles.clear();
    this.selectedProfile = null;
  }
  
  public void selectGameProfile(GameProfile profile)
    throws RequestException
  {
    if (!this.loggedIn) {
      throw new RequestException("Cannot change game profile while not logged in.");
    }
    if (this.selectedProfile != null) {
      throw new RequestException("Cannot change game profile when it is already selected.");
    }
    if ((profile != null) && (this.profiles.contains(profile)))
    {
      RefreshRequest request = new RefreshRequest(this.clientToken, this.accessToken, profile);
      RefreshResponse response = (RefreshResponse)HTTP.makeRequest(this.proxy, "https://authserver.mojang.com/refresh", request, RefreshResponse.class);
      if (response.clientToken.equals(this.clientToken))
      {
        this.accessToken = response.accessToken;
        this.selectedProfile = response.selectedProfile;
      }
      else
      {
        throw new RequestException("Server requested we change our client token. Don't know how to handle this!");
      }
    }
    else
    {
      throw new IllegalArgumentException("Invalid profile '" + profile + "'.");
    }
  }
  
  public String toString()
  {
    return "UserAuthentication{clientToken=" + this.clientToken + ", username=" + this.username + ", accessToken=" + this.accessToken + ", loggedIn=" + this.loggedIn + ", profiles=" + this.profiles + ", selectedProfile=" + this.selectedProfile + "}";
  }
  
  private void loginWithPassword()
    throws RequestException
  {
    if ((this.username == null) || (this.username.isEmpty())) {
      throw new InvalidCredentialsException("Invalid username.");
    }
    if ((this.password == null) || (this.password.isEmpty())) {
      throw new InvalidCredentialsException("Invalid password.");
    }
    AuthenticationRequest request = new AuthenticationRequest(this.username, this.password, this.clientToken);
    AuthenticationResponse response = (AuthenticationResponse)HTTP.makeRequest(this.proxy, "https://authserver.mojang.com/authenticate", request, AuthenticationResponse.class);
    if (response.clientToken.equals(this.clientToken))
    {
      if ((response.user != null) && (response.user.id != null)) {
        this.id = response.user.id;
      } else {
        this.id = this.username;
      }
      this.loggedIn = true;
      this.accessToken = response.accessToken;
      this.profiles = (response.availableProfiles != null ? Arrays.asList(response.availableProfiles) : Collections.emptyList());
      this.selectedProfile = response.selectedProfile;
      this.properties.clear();
      if ((response.user != null) && (response.user.properties != null)) {
        this.properties.addAll(response.user.properties);
      }
    }
    else
    {
      throw new RequestException("Server requested we change our client token. Don't know how to handle this!");
    }
  }
  
  private void loginWithToken()
    throws RequestException
  {
    if ((this.id == null) || (this.id.isEmpty()))
    {
      if ((this.username == null) || (this.username.isEmpty())) {
        throw new InvalidCredentialsException("Invalid uuid and username.");
      }
      this.id = this.username;
    }
    if ((this.accessToken == null) || (this.accessToken.equals(""))) {
      throw new InvalidCredentialsException("Invalid access token.");
    }
    RefreshRequest request = new RefreshRequest(this.clientToken, this.accessToken, null);
    RefreshResponse response = (RefreshResponse)HTTP.makeRequest(this.proxy, "https://authserver.mojang.com/refresh", request, RefreshResponse.class);
    if (response.clientToken.equals(this.clientToken))
    {
      if ((response.user != null) && (response.user.id != null)) {
        this.id = response.user.id;
      } else {
        this.id = this.username;
      }
      this.loggedIn = true;
      this.accessToken = response.accessToken;
      this.profiles = (response.availableProfiles != null ? Arrays.asList(response.availableProfiles) : Collections.emptyList());
      this.selectedProfile = response.selectedProfile;
      this.properties.clear();
      if ((response.user != null) && (response.user.properties != null)) {
        this.properties.addAll(response.user.properties);
      }
    }
    else
    {
      throw new RequestException("Server requested we change our client token. Don't know how to handle this!");
    }
  }
  
  private static class Agent
  {
    private String name;
    private int version;
    
    protected Agent(String name, int version)
    {
      this.name = name;
      this.version = version;
    }
  }
  
  private static class User
  {
    public String id;
    public List<GameProfile.Property> properties;
  }
  
  private static class AuthenticationRequest
  {
    private AuthenticationService.Agent agent;
    private String username;
    private String password;
    private String clientToken;
    private boolean requestUser;
    
    protected AuthenticationRequest(String username, String password, String clientToken)
    {
      this.agent = new AuthenticationService.Agent("Minecraft", 1);
      this.username = username;
      this.password = password;
      this.clientToken = clientToken;
      this.requestUser = true;
    }
  }
  
  private static class RefreshRequest
  {
    private String clientToken;
    private String accessToken;
    private GameProfile selectedProfile;
    private boolean requestUser;
    
    protected RefreshRequest(String clientToken, String accessToken, GameProfile selectedProfile)
    {
      this.clientToken = clientToken;
      this.accessToken = accessToken;
      this.selectedProfile = selectedProfile;
      this.requestUser = true;
    }
  }
  
  private static class InvalidateRequest
  {
    private String clientToken;
    private String accessToken;
    
    protected InvalidateRequest(String clientToken, String accessToken)
    {
      this.clientToken = clientToken;
      this.accessToken = accessToken;
    }
  }
  
  private static class AuthenticationResponse
  {
    public String accessToken;
    public String clientToken;
    public GameProfile selectedProfile;
    public GameProfile[] availableProfiles;
    public AuthenticationService.User user;
  }
  
  private static class RefreshResponse
  {
    public String accessToken;
    public String clientToken;
    public GameProfile selectedProfile;
    public GameProfile[] availableProfiles;
    public AuthenticationService.User user;
  }
}
