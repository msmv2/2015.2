package ru.mirea.oop.practice.coursej.s131250;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit.Call;
import ru.mirea.oop.practice.coursej.vk.Account;
import ru.mirea.oop.practice.coursej.vk.Messages;
import ru.mirea.oop.practice.coursej.vk.Result;
import ru.mirea.oop.practice.coursej.vk.entities.Contact;
import ru.mirea.oop.practice.coursej.vk.ext.ServiceExtension;

import java.io.IOException;

public final class VkWolframInteractionService extends ServiceExtension {
    private static final Logger logger = LoggerFactory.getLogger(VkWolframInteractionService.class);
    private final Messages msgApi;
    private final Account accApi;

    public VkWolframInteractionService() throws Exception {
        super("vk.services.Wolfram");
        this.accApi = api.getAccounts();
        this.msgApi = api.getMessages();
    }

    @Override
    protected void doEvent(Event event) {
        Call<Result<Integer>> onlineCall = accApi.setOnline(0);
        try {
            Result.call(onlineCall);
        } catch (IOException ex) {
            logger.error("Ошибка выхода в онлайн", ex);
        }
        switch (event.type) {
            case MESSAGE_RECEIVE: {
                Message msg = (Message) event.object;
                Contact contact = msg.contact;
                if (msg.isOutbox()) {
                    logger.debug("Сообщение для " + Contact.viewerString(contact) +
                            ", не следует на него отвечать оно исходящее");
                    logger.debug("Текст сообщения: " + msg.text);
                    break;
                }
                logger.debug("Получили сообщение от " + Contact.viewerString(contact));

                    if (msg.text.startsWith("wi ")) {
                        WAMessage message = null;
                        Call<Result<Integer>> activityCall = msgApi.setActivity(msg.contact.id, "typing");
                        try {
                            Result.call(activityCall);
                        } catch (IOException ex) {
                            logger.error("Ошибка отправки статуса \"typing\"", ex);
                        }

                        try {
                            message = WAAction.processWAMessage(msg.text.split("wi ")[1], api);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (message != null) {
                            Call<Result<Integer>> call = msgApi.send(
                                    contact.id,
                                    null,
                                    null,
                                    null,
                                    message.text,
                                    null,
                                    null,
                                    null,
                                    message.attachment,
                                    null,
                                    null
                            );
                            try {
                                Integer idMessage = Result.call(call);
                                logger.debug("Сообщение отправлено " + idMessage);
                            } catch (IOException ex) {
                                logger.error("Ошибка отправки сообщения", ex);
                            }
                        }

                    }
                break;
            }
            default:
                logger.debug("" + (event.object == null ? event.type : event.type + "|" + event.object));
        }
    }

    @Override
    protected boolean init() {
        return true;
    }

    @Override
    public String description() {
        return "Сервис взаимодействия с WolframAlpha";
    }
}
