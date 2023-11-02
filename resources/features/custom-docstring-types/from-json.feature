Feature: Burpless Supports Custom DocString Types, Such as JSON

  Scenario: Populate state with values from custom DocString inputs and compare them for equality
    Given that my state starts out as an empty map
    And I want to store the following in my state's :json-sample key
    """json
    [
      {
        "id": "65439b9690b50f79a0752ed6",
        "email": "terry_house@vortexaco.bargains",
        "username": "terry91",
        "profile": {
          "name": "Terry House",
          "company": "Vortexaco",
          "dob": "1991-03-25",
          "address": "1 Regent Place, Sussex, New York",
          "location": {
            "lat": 88.908225,
            "long": 125.754369
          },
          "about": "Excepteur voluptate veniam quis laborum sunt. Anim dolor proident anim cupidatat magna laboris aliqua qui sint ea esse elit."
        },
        "apiKey": "c24efd57-e3fe-44f6-bb6e-df3063143b81",
        "roles": [
          "member"
        ],
        "createdAt": "2014-04-12T03:29:52.355Z",
        "updatedAt": "2014-04-13T03:29:52.355Z"
      }
    ]
    """
    And I want to store the following in my state's :edn-sample key
    """edn
    [{:id         "65439b9690b50f79a0752ed6"
      :email      "terry_house@vortexaco.bargains"
      :username   "terry91"
      :profile    {:name     "Terry House"
                   :company  "Vortexaco"
                   :dob      "1991-03-25"
                   :address  "1 Regent Place, Sussex, New York"
                   :location {:lat 88.908225 :long 125.754369}
                   :about    "Excepteur voluptate veniam quis laborum sunt. Anim dolor proident anim cupidatat magna laboris aliqua qui sint ea esse elit."}
      :api-key    "c24efd57-e3fe-44f6-bb6e-df3063143b81"
      :roles      ["member"]
      :created-at "2014-04-12T03:29:52.355Z"
      :updated-at "2014-04-13T03:29:52.355Z"}]
    """
    When I compare the my state's :json-sample and :edn-sample keys to each other for equality, storing the result in :result
    Then my state's :result equality value should be true
