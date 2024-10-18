package ru.x5.dnd.telegrambot.service;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.x5.dnd.telegrambot.exception.BotLogicException;
import ru.x5.dnd.telegrambot.model.Game;
import ru.x5.dnd.telegrambot.model.GameRegistration;
import ru.x5.dnd.telegrambot.model.RegistrationType;
import ru.x5.dnd.telegrambot.repository.GameRepository;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

@Service
public class GameService {

    private final GameRepository gameRepository;
    private final MessageSource messageSource;

    public GameService(GameRepository gameRepository, MessageSource messageSource) {
        this.gameRepository = gameRepository;
        this.messageSource = messageSource;
    }

    @Transactional
    public void createGame(String chatId, String messageId, String messageThreadId, String author, LocalDate gameDate) {
        Optional<Game> gameOptional = gameRepository.findFirstByChatIdAndMessageIdAndMessageThreadId(chatId, messageId, messageThreadId);
        if (gameOptional.isPresent()) {
            throw new BotLogicException(messageSource.getMessage("error.game-exists", null, Locale.getDefault()));
        }

        Game game = new Game();
        game.setChatId(chatId);
        game.setMessageId(messageId);
        game.setMessageThreadId(messageThreadId);
        game.setAuthor(author);
        game.setGameDate(gameDate);
        gameRepository.save(game);
    }

    @Transactional
    public Game addUserToGame(String chatId, String messageId, String messageThreadId, String userName, RegistrationType registrationType) {
        Optional<Game> gameOptional = gameRepository.findFirstByChatIdAndMessageIdAndMessageThreadId(chatId, messageId, messageThreadId);
        if (gameOptional.isEmpty()) {
            throw new BotLogicException(messageSource.getMessage("error.game-not-exists", null, Locale.getDefault()));
        }
        Game game = gameOptional.get();

        if (game.getGameRegistrations().stream().anyMatch(r -> userName.equals(r.getGamerName()))) {
            throw new BotLogicException(messageSource.getMessage("error.game-registration-exists", new Object[]{userName}, Locale.getDefault()));
        }

        GameRegistration registration = new GameRegistration();
        registration.setGame(game);
        registration.setGamerName(userName);
        registration.setRegistrationType(registrationType);

        game.getGameRegistrations().add(registration);

        return gameRepository.save(game);
    }

    @Transactional
    public Game removeUserFromGame(String chatId, String messageId, String messageThreadId, String userName) {
        Optional<Game> gameOptional = gameRepository.findFirstByChatIdAndMessageIdAndMessageThreadId(chatId, messageId, messageThreadId);
        if (gameOptional.isEmpty()) {
            throw new BotLogicException(messageSource.getMessage("error.game-not-exists", null, Locale.getDefault()));
        }
        Game game = gameOptional.get();
        if (!game.getGameRegistrations().removeIf(registration -> registration.getGamerName().equals(userName))) {
            throw new BotLogicException(messageSource.getMessage("error.game-registration-not-exists", new Object[]{userName}, Locale.getDefault()));
        }

        return gameRepository.save(game);
    }
}