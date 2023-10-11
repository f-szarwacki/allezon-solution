package com.fszarwacki.allezon.processor;

import com.fszarwacki.allezon.common.Action;
import com.fszarwacki.allezon.common.UserTagEvent;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
public class RemoveOldTagsService {
  private static final Logger log = LoggerFactory.getLogger(RemoveOldTagsService.class);
  
  private final UserTagEventRepository userTagEventRepository;
  private final LinkedBlockingQueue<UserTagEvent> batchUserTagEventsToDelete = new LinkedBlockingQueue<>();

  @Autowired
  public RemoveOldTagsService(UserTagEventRepository userTagEventRepository, Environment env) {
    this.userTagEventRepository = userTagEventRepository;
  }

  /* tagsReturned need to be already sorted decreasing by time. */
  @Async
  public void removeOldTags(List<UserTagEvent> tagsReturned) {
    List<UserTagEvent> viewsToDelete = tagsReturned.stream()
            .filter(userTagEvent -> userTagEvent.getAction() == Action.VIEW)
            .skip(200)
            .collect(Collectors.toList());

    List<UserTagEvent> buysToDelete = tagsReturned.stream()
            .filter(userTagEvent -> userTagEvent.getAction() == Action.BUY)
            .skip(200)
            .collect(Collectors.toList());

    batchUserTagEventsToDelete.addAll(viewsToDelete);
    batchUserTagEventsToDelete.addAll(buysToDelete);

    if (batchUserTagEventsToDelete.size() > 1000) {
      List<UserTagEvent> temp = new LinkedList<>();
      batchUserTagEventsToDelete.drainTo(temp);
      userTagEventRepository.deleteAll(temp);
      temp.clear();
    }
  }
}
