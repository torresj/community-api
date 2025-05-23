package com.torresj.community.exceptions;

public class CommunityNotFoundException extends Exception {
  public CommunityNotFoundException(Long id) {
    super("Community " + id + " not found");
  }
}
