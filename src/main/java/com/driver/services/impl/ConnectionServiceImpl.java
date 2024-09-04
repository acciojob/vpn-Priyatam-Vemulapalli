package com.driver.services.impl;

import com.driver.model.*;
import com.driver.repository.ConnectionRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Optional;

@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Autowired
    UserRepository userRepository2;
    @Autowired
    ServiceProviderRepository serviceProviderRepository2;
    @Autowired
    ConnectionRepository connectionRepository2;

    @Override
    public User connect(int userId, String countryName) throws Exception{
        User user = userRepository2.findById(userId).get();
        if(ObjectUtils.isEmpty(user)){
            throw new Exception("User not found");
        }

        if(user.getMaskedIp()!=null){
            throw new Exception("Already connected");
        }
        if(user.getOriginalCountry().getCountryName().toString().equalsIgnoreCase(countryName)){
            return user;
        }
        List<ServiceProvider> serviceProviders = user.getServiceProviderList();
        ServiceProvider serviceProvider = new ServiceProvider();
        Country country = new Country();
        int c=Integer.MAX_VALUE;

        for(ServiceProvider serviceProvider1: serviceProviders){
            List<Country> countryList = serviceProvider1.getCountryList();
            for(Country country1: countryList){
                if(country1.getCountryName().toString().equalsIgnoreCase(countryName) && c>serviceProvider1.getId()){
                    country=country1;
                    serviceProvider=serviceProvider1;
                    c=serviceProvider1.getId();
                }
            }
        }
        if(serviceProvider!=null){
            Connection connection =new Connection();
            connection.setUser(user);
            connection.setServiceProvider(serviceProvider);

            String countryCode = country.getCode();
            int providerId = serviceProvider.getId();
            String mask= countryCode+"."+providerId+"."+userId;
            user.setMaskedIp(mask);
            user.setConnected(true);
            user.getServiceProviderList().add(serviceProvider);
            serviceProvider.getUsers().add(user);
            serviceProviderRepository2.save(serviceProvider);
            userRepository2.save(user);

        }

        return user;
    }
    @Override
    public User disconnect(int userId) throws Exception {
        User user = userRepository2.findById(userId).get();

        if(!ObjectUtils.isEmpty(user)){
            if(user.getConnected()==false){
                throw new Exception("Already disconnected");
            }
            user.setMaskedIp(null);
            user.setConnected(false);
            userRepository2.save(user);

        }
        else{
            throw new Exception("User not found");
        }
        return user;
    }

    @Override
    public User communicate(int senderId, int receiverId) throws Exception {

        User sender= userRepository2.findById(senderId).get();
        User receiver=userRepository2.findById(receiverId).get();
        if(ObjectUtils.isEmpty(sender) || ObjectUtils.isEmpty(receiver)){
            throw new Exception("Invalid Ids");
        }
        if(receiver.getMaskedIp()!=null){
            String countryCode = receiver.getMaskedIp().substring(0,3);

            if(sender.getOriginalCountry().getCode().equalsIgnoreCase(countryCode)){
                return sender; //they can communicate without connection
            }
            else{
                String countryName="";
                if(countryCode.equals(CountryName.AUS.toCode())){
                    countryName= CountryName.AUS.toString();
                }
                if(countryCode.equals(CountryName.CHI.toCode())){
                    countryName= CountryName.CHI.toString();
                }
                if(countryCode.equals(CountryName.IND.toCode())){
                    countryName= CountryName.IND.toString();
                }
                if(countryCode.equals(CountryName.JPN.toCode())){
                    countryName= CountryName.JPN.toString();
                }
                if(countryCode.equals(CountryName.USA.toCode())){
                    countryName= CountryName.USA.toString();
                }
                try {
                    return connect(senderId, countryName);
                } catch (Exception e) {
                    throw new Exception("Cannot establish communication");
                }
            }
        }
        else{
            if(receiver.getOriginalCountry().equals(sender.getOriginalCountry())){
                return sender;
            }
            else{
                String countryName= receiver.getOriginalCountry().getCountryName().toString();
                try {
                    return connect(senderId, countryName);
                } catch (Exception e) {
                    throw new Exception("Cannot establish communication");
                }
            }
        }
    }
}
