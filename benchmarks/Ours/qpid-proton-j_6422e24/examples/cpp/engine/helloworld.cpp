/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

#include "proton/event.hpp"
#include "proton/handler.hpp"
#include "proton/url.hpp"
#include "proton/io.hpp"

#include <iostream>

class hello_world : public proton::handler {
  private:
    std::string address_;

  public:
    hello_world(const std::string& address) : address_(address) {}

    void on_start(proton::event &e) {
        e.connection().open();
        e.connection().open_receiver(address_);
        e.connection().open_sender(address_);
    }

    void on_sendable(proton::event &e) {
        proton::message m("Hello World!");
        e.sender().send(m);
        e.sender().close();
    }

    void on_message(proton::event &e) {
        std::cout << e.message().body() << std::endl;
        e.connection().close();
    }
};

int main(int argc, char **argv) {
    try {
        proton::url url(argc > 1 ? argv[1] : "127.0.0.1:5672/examples");

        hello_world hw(url.path());
        proton::io::socket_engine(url, hw).run();

        return 0;
    } catch (const std::exception& e) {
        std::cerr << e.what() << std::endl;
    }

    return 1;
}
