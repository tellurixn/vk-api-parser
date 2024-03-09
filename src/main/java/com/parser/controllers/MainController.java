package com.parser.controllers;


import com.vk.api.sdk.actions.Friends;
import com.vk.api.sdk.client.ApiRequest;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.ServiceActor;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;

import com.vk.api.sdk.objects.base.NameCase;
import com.vk.api.sdk.objects.base.Sex;
import com.vk.api.sdk.objects.users.Fields;
import com.vk.api.sdk.objects.users.User;
import com.vk.api.sdk.objects.users.responses.GetResponse;
import com.vk.api.sdk.queries.users.UsersGetQuery;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
public class MainController {


    static String serviceKey = "service_key";
    static int appId = app_id;

    @GetMapping("/")
    public String index(Model model){
        return "index";
    }

    @PostMapping("/")
    public String sendUserID(@RequestParam String vkUserID,
                             Model model) throws ClientException, ApiException {

        TransportClient transportClient = new HttpTransportClient();
        VkApiClient vk = new VkApiClient(transportClient);
        ServiceActor serviceActor = new ServiceActor(appId, serviceKey);

        Long userIntID = 0L;
        String userUniversityName = "";
        String userName = "";
        String userLastName = "";
        List<GetResponse> friendsByUniversity = new ArrayList<>();
        List<GetResponse> relatives = new ArrayList<>();


        //Получение информации о пользователе
        List<GetResponse> users = vk.users()
                .get(serviceActor)
                .userIds(vkUserID)
                .fields(Fields.UNIVERSITIES, Fields.EDUCATION, Fields.SEX)
                .nameCase(NameCase.GENITIVE)
                .execute();

        /*Получение числового ID пользователя
        фамилии и имени пользователя
        и университета*/
        for (GetResponse user : users) {
            if(user.getIsClosed())
                return "error";
            userIntID = user.getId();
            userUniversityName = user.getUniversityName();
            userLastName = user.getLastName();
            userName = user.getFirstName() + " " + userLastName;
        }

        //Получение списка друзей пользователя
        com.vk.api.sdk.objects.friends.responses.GetResponse userFriends = vk.friends()
                .get(serviceActor)
                .userId(userIntID).fields()
                .execute();

        //Получение числовых ID пользователей для поиска по месту учебы
        List<String> friendsIds = new ArrayList<>();
        for(Long id : userFriends.getItems())
            friendsIds.add(String.valueOf(id));

        //Получение информации о друзьях, включая информацию о месте учебы
        List<GetResponse> friends = vk.users()
                .get(serviceActor)
                .userIds(friendsIds)
                .fields(Fields.UNIVERSITIES, Fields.EDUCATION, Fields.SEX)
                .nameCase(NameCase.GENITIVE)
                .execute();

        //Формирование списка друзей, которые обучаются в университете пользователя
        for (GetResponse friend : friends) {
            if(friend.getUniversityName() != null && friend.getUniversityName().equals(userUniversityName))
                friendsByUniversity.add(friend);


            //поиск близких родственников
            if(users.getFirst().getSex() == Sex.MALE) {
                if (friend.getLastName().equals(userLastName) ||
                        friend.getLastName().equals(userLastName + 'а'))
                    relatives.add(friend);
            }
            else {
                if (friend.getLastName().equals(userLastName) ||
                        friend.getLastName().equals(userLastName.substring(0, userLastName.length() - 1)))
                    relatives.add(friend);
            }
        }

        System.out.println("RELATIVES: " + relatives.size());
        System.out.println("SEX: " + users.getFirst().getSex());

        //Передача найденных данных на страницу с результатом
        model.addAttribute("userName", userName);
        model.addAttribute("friends", friendsByUniversity);
        model.addAttribute("relatives", relatives);

        return "result";
    }

}
