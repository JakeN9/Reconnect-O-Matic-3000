package org.spacehq.mc.auth.service;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.spacehq.mc.auth.data.GameProfile;
import org.spacehq.mc.auth.exception.profile.ProfileNotFoundException;
import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.auth.util.HTTP;

public class ProfileService
{
  private static final String BASE_URL = "https://api.mojang.com/profiles/";
  private static final String SEARCH_URL = "https://api.mojang.com/profiles/minecraft";
  private static final int MAX_FAIL_COUNT = 3;
  private static final int DELAY_BETWEEN_PAGES = 100;
  private static final int DELAY_BETWEEN_FAILURES = 750;
  private static final int PROFILES_PER_REQUEST = 100;
  private Proxy proxy;
  
  public ProfileService()
  {
    this(Proxy.NO_PROXY);
  }
  
  public ProfileService(Proxy proxy)
  {
    if (proxy == null) {
      throw new IllegalArgumentException("Proxy cannot be null.");
    }
    this.proxy = proxy;
  }
  
  public void findProfilesByName(String[] names, ProfileLookupCallback callback)
  {
    findProfilesByName(names, callback, false);
  }
  
  public void findProfilesByName(String[] names, final ProfileLookupCallback callback, boolean async)
  {
    final Set<String> criteria = new HashSet();
    for (String name : names) {
      if ((name != null) && (!name.isEmpty())) {
        criteria.add(name.toLowerCase());
      }
    }
    Object runnable = new Runnable()
    {
      public void run()
      {
        for (Set<String> request : ProfileService.partition(criteria, 100))
        {
          Exception error = null;
          int failCount = 0;
          boolean tryAgain = true;
          while ((failCount < 3) && (tryAgain))
          {
            tryAgain = false;
            try
            {
              GameProfile[] profiles = (GameProfile[])HTTP.makeRequest(ProfileService.this.proxy, "https://api.mojang.com/profiles/minecraft", request, GameProfile[].class);
              failCount = 0;
              missing = new HashSet(request);
              for (GameProfile profile : profiles)
              {
                missing.remove(profile.getName().toLowerCase());
                callback.onProfileLookupSucceeded(profile);
              }
              for (??? = missing.iterator(); ((Iterator)???).hasNext();)
              {
                String name = (String)((Iterator)???).next();
                callback.onProfileLookupFailed(new GameProfile((UUID)null, name), new ProfileNotFoundException("Server could not find the requested profile."));
              }
              try
              {
                Thread.sleep(100L);
              }
              catch (InterruptedException localInterruptedException2) {}
            }
            catch (RequestException e)
            {
              Set<String> missing;
              error = e;
              failCount++;
              if (failCount >= 3)
              {
                for (String name : request) {
                  callback.onProfileLookupFailed(new GameProfile((UUID)null, name), error);
                }
              }
              else
              {
                try
                {
                  Thread.sleep(750L);
                }
                catch (InterruptedException localInterruptedException1) {}
                tryAgain = true;
              }
            }
          }
        }
      }
    };
    if (async) {
      new Thread((Runnable)runnable, "ProfileLookupThread").start();
    } else {
      ((Runnable)runnable).run();
    }
  }
  
  private static Set<Set<String>> partition(Set<String> set, int size)
  {
    List<String> list = new ArrayList(set);
    Set<Set<String>> ret = new HashSet();
    for (int i = 0; i < list.size(); i += size)
    {
      Set<String> s = new HashSet();
      s.addAll(list.subList(i, Math.min(i + size, list.size())));
      ret.add(s);
    }
    return ret;
  }
  
  public static abstract interface ProfileLookupCallback
  {
    public abstract void onProfileLookupSucceeded(GameProfile paramGameProfile);
    
    public abstract void onProfileLookupFailed(GameProfile paramGameProfile, Exception paramException);
  }
}
