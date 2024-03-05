package com.proxy.server;

import com.proxy.server.entities.Admin;
import com.proxy.server.repositories.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCrypt;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {
		SecurityAutoConfiguration.class
})
public class ServerApplication {
	@Autowired
	private AdminRepository adminRepository;
	public static void main(String[] args) {
		SpringApplication.run(ServerApplication.class, args);
	}

	//Set the admin password
	@Bean
	public CommandLineRunner setAdminPassword() {
		return args -> {
			if(args.length>0 && !args[0].isEmpty()) {
				String salt = BCrypt.gensalt();
				String hashedPassword = BCrypt.hashpw(args[0], salt);
				adminRepository.save(new Admin(hashedPassword, salt));
				System.out.println("Admin password set: " + args[0]);
			}
		};
	}

}
