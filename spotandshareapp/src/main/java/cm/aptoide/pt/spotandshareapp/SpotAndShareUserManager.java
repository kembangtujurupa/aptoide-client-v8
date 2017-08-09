package cm.aptoide.pt.spotandshareapp;

/**
 * Created by filipe on 23-06-2017.
 */

public class SpotAndShareUserManager {

  private SpotAndShareApplication spotAndShareApplication;
  private SpotAndShareUserPersister persister;

  public SpotAndShareUserManager(SpotAndShareApplication spotAndShareApplication,
      SpotAndShareUserPersister persister) {
    this.spotAndShareApplication = spotAndShareApplication;
    this.persister = persister;
  }

  public void createUser(SpotAndShareUser user) {
    persister.save(user);
    updateFriendOnSpotAndShare();
  }

  public SpotAndShareUser getUser() {
    return persister.get();
  }

  public void updateUser(SpotAndShareUser user) {
    persister.save(user);
    updateFriendOnSpotAndShare();
  }

  private void updateFriendOnSpotAndShare() {
    spotAndShareApplication.updateFriendProfileOnSpotAndShare();
  }
}