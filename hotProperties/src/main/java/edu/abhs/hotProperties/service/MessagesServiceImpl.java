package edu.abhs.hotProperties.service;

import edu.abhs.hotProperties.entities.Messages;
import edu.abhs.hotProperties.entities.User;
import edu.abhs.hotProperties.repository.MessagesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessagesServiceImpl implements  MessagesService {

    MessagesRepository messagesRepository;

    @Autowired
    public MessagesServiceImpl(MessagesRepository messagesRepository) {
        this.messagesRepository = messagesRepository;
    }

    @Override
    public void addMessages(Messages messages) {
        messagesRepository.save(messages);
    }


}
