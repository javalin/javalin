/**
 * This file contains the json definitions required for the test
 */
package io.javalin.plugin.openapi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import io.javalin.plugin.json.JavalinJackson
import io.javalin.plugin.openapi.jackson.JacksonToJsonMapper
import io.swagger.v3.oas.models.OpenAPI
import org.intellij.lang.annotations.Language

fun configureJacksonToJsonMapper() {
    JacksonToJsonMapper.defaultObjectMapper
            .enable(SerializationFeature.INDENT_OUTPUT)
}

fun String.formatJson(): String {
    configureJacksonToJsonMapper()
    val node = JavalinJackson.fromJson(this, JsonNode::class.java)
    return JacksonToJsonMapper().map(node)
}

fun OpenAPI.asJsonString(): String = JacksonToJsonMapper().map(this)

// This variable is needed for the jsons. That's how I can avoid the ugly """${"$"}""".
val ref = "\$ref"

@Language("JSON")
val userOpenApiSchema = """
{
   "required":[
      "name"
   ],
   "type":"object",
   "properties":{
      "name":{
         "type":"string"
      },
      "address":{
          "$ref": "#/components/schemas/Address"
      }
   }
}
""".formatJson()

@Language("JSON")
val addressOpenApiSchema = """
{
   "required": [
      "number",
      "street"
   ],
   "type":"object",
   "properties": {
      "street": {
         "type": "string"
      },
      "number": {
         "type": "integer",
         "format": "int32"
      }
   }
}
""".formatJson()

@Language("JSON")
val complexExampleUsersGetResponsesJson = """
{
  "200": {
    "description": "OK",
    "content": {
      "application/json": {
        "schema": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/User"
          }
        }
      }
    }
  }
}
""".formatJson()

@Language("JSON")
val queryBeanExample = """
    {
      "openapi" : "3.0.1",
      "info" : {
        "title" : "Example",
        "version" : "1.0.0"
      },
      "paths" : {
        "/test" : {
          "get" : {
            "summary" : "Get test",
            "operationId" : "getTest",
            "parameters" : [ {
              "name" : "user",
              "in" : "query",
              "schema" : {
                "$ref" : "#/components/schemas/User"
              }
            } ],
            "responses" : {
              "200" : {
                "description" : "OK",
                "content" : {
                  "application/json" : {
                    "schema" : {
                      "$ref" : "#/components/schemas/User"
                    }
                  }
                }
              }
            }
          }
        }
      },
      "components" : {
        "schemas" : {
          "Address" : {
            "required" : [ "number", "street" ],
            "type" : "object",
            "properties" : {
              "street" : {
                "type" : "string"
              },
              "number" : {
                "type" : "integer",
                "format" : "int32"
              }
            }
          },
          "User" : {
            "required" : [ "name" ],
            "type" : "object",
            "properties" : {
              "name" : {
                "type" : "string"
              },
              "address" : {
                "$ref" : "#/components/schemas/Address"
              }
            }
          }
        }
      }
    }
""".formatJson()

@Language("JSON")
val simpleExample = """
  {
    "openapi": "3.0.1",
    "info": {
      "title": "Example",
      "version": "1.0.0"
    },
    "paths": {
      "/test": {
        "get": {
          "summary": "Get test",
          "operationId": "getTest",
          "responses" : {
            "200" : {
              "description" : "OK"
            }
          }
        }
      }
    },
    "components": {}
  }
""".formatJson()

@Language("JSON")
val simpleExample1 = """
  {
    "openapi": "3.0.1",
    "info": {
      "title": "Example",
      "version": "1.0.0"
    },
    "paths": {
      "/test1": {
        "get": {
          "summary": "Get test1",
          "operationId": "getTest1",
          "responses" : {
            "200" : {
              "description" : "OK"
            }
          }
        }
      }
    },
    "components": {}
  }
""".formatJson()

@Language("JSON")
val simpleExample2 = """
  {
    "openapi": "3.0.1",
    "info": {
      "title": "Example",
      "version": "1.0.0"
    },
    "paths": {
      "/test2": {
        "get": {
          "summary": "Get test2",
          "operationId": "getTest2",
          "responses" : {
            "200" : {
              "description" : "OK"
            }
          }
        }
      }
    },
    "components": {}
  }
""".formatJson()

@Language("JSON")
val simplePutExample = """
  {
    "openapi" : "3.0.1",
    "info" : {
      "title" : "Example",
      "version" : "1.0.0"
    },
    "paths" : {
      "/put" : {
        "put" : {
          "summary" : "Put put",
          "operationId" : "putPut",
          "responses" : {
            "200" : {
            "description" : "OK"
            }
          }
        }
      }
    },
    "components" : { }
  }
""".formatJson()

@Language("JSON")
val simpleDeleteExample = """
  {
    "openapi" : "3.0.1",
    "info" : {
      "title" : "Example",
      "version" : "1.0.0"
    },
    "paths" : {
      "/delete" : {
        "put" : {
          "summary" : "Put delete",
          "operationId" : "putDelete",
          "responses" : {
            "200" : {
              "description" : "OK"
            }
          }
        }
      }
    },
    "components" : { }
  }
""".formatJson()

@Language("JSON")
val simpleExampleWithDescription = """
  {
    "openapi": "3.0.1",
    "info": {
      "title": "Example",
      "version": "1.0.0"
    },
    "paths": {
      "/test": {
        "get": {
          "summary": "Get test",
          "description" : "Test1",
          "operationId": "getTest",
          "responses" : {
            "200" : {
              "description" : "OK"
            }
          }
        }
      }
    },
    "components": {}
  }
""".formatJson()

@Language("JSON")
val simpleExampleWithPrimitiveQueryParam = """
  {
    "openapi": "3.0.1",
    "info": {
      "title": "Example",
      "version": "1.0.0"
    },
    "paths": {
      "/test": {
        "get": {
          "summary": "Get test",
          "operationId": "getTest",
          "parameters" : [ {
            "name" : "id",
            "in" : "query",
            "schema" : {
              "type": "integer",
              "format": "int64"
            }
          } ],
          "responses" : {
            "200" : {
              "description" : "Default response"
            }
          }
        }
      }
    },
    "components": {}
  }
""".formatJson()

@Language("JSON")
val simpleExampleWithMultipleGets = """
  {
    "openapi": "3.0.1",
    "info": {
      "title": "Example",
      "version": "1.0.0"
    },
    "paths": {
      "/test1": {
        "get": {
          "summary": "Get test1",
          "description": "Test1",
          "operationId": "getTest1",
          "responses" : {
            "200" : {
              "description" : "OK"
            }
          }
        }
      },
      "/test2": {
        "get": {
          "summary": "Get test2",
          "description": "Test2",
          "operationId": "getTest2",
          "responses" : {
            "200" : {
              "description" : "OK"
            }
          }
        }
      }
    },
    "components": {}
  }
""".formatJson()

@Language("JSON")
val simpleExampleWithRepeatableQueryParam = """
  {
  "openapi" : "3.0.1",
    "info" : {
      "title" : "Example",
      "version" : "1.0.0"
    },
    "paths" : {
      "/test" : {
        "get" : {
          "summary" : "Get test",
          "operationId" : "getTest",
          "parameters" : [ {
            "name" : "id",
            "in" : "query",
            "schema" : {
              "type" : "array",
              "items" : {
                "type" : "integer",
                "format" : "int64"
              }
            }
          } ],
          "responses" : {
            "200" : {
              "description" : "Default response"
            }
          }
        }
      }
    },
    "components" : { }
}
""".formatJson()

@Language("JSON")
val simpleExampleWithMultipleHttpMethods = """
  {
    "openapi": "3.0.1",
    "info": {
      "title": "Example",
      "version": "1.0.0"
    },
    "paths": {
      "/test": {
        "get": {
          "summary": "Get test",
          "description": "Test1",
          "operationId": "getTest",
          "responses" : {
            "200" : {
              "description" : "OK"
            }
          }
        },
        "post": {
          "summary": "Post test",
          "description": "Test2",
          "operationId": "postTest",
          "responses" : {
            "200" : {
              "description" : "OK"
            }
          }
        }
      }
    },
    "components": {}
  }
""".formatJson()

@Language("JSON")
val provideRouteExampleJson = """
  {
    "openapi": "3.0.1",
    "info": {
      "title": "Example",
      "version": "1.0.0"
    },
    "paths": {
      "/test": {
        "get": {
          "summary": "Get test",
          "operationId": "getTest",
          "responses" : {
            "200" : {
              "description" : "Default response"
            }
          }
        }
      }
    },
    "components": {}
  }
""".formatJson()

@Language("JSON")
val complexExampleJson = """
{
  "openapi": "3.0.1",
  "info": {
    "title": "Example",
    "version": "1.0.0"
  },
  "externalDocs": {
    "description": "Find more info here",
    "url": "https://external-documentation.info"
  },
  "servers": [
    {
      "url": "https://app.example",
      "description": "My example app"
    }
  ],
  "security" : [ {
    "http" : [ ],
    "token" : [ ]
  } ],
  "tags": [
    {
      "name": "user",
      "description": "User operations"
    }
  ],
  "paths": {
    "/unimplemented": {
      "get": {
        "summary": "This path is not implemented in javalin",
        "responses" : {
          "200" : {
            "description" : "Default response"
          }
        }
      }
    },
    "/user": {
      "description": "Some additional information for the /user endpoint",
      "get": {
        "tags": ["user"],
        "summary": "Get current user",
        "description": "Get a specific user",
        "operationId": "getCurrentUser",
        "responses": {
          "200": {
            "description": "Request successful",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/User"
                }
              },
              "application/xml": {
                "schema": {
                  "$ref": "#/components/schemas/User"
                }
              }
            }
          }
        },
        "deprecated": true
      },
      "put": {
        "tags": ["user"],
        "summary": "Put user",
        "operationId": "putUser",
        "requestBody": {
          "description": "body description",
          "content": {
            "text/plain": {
              "schema": {
                "type": "string"
              }
            },
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/User"
              }
            },
            "application/xml": {
              "schema": {
                "$ref": "#/components/schemas/User"
              }
            },
            "application/octet-stream": {
              "schema": {
                "type": "string",
                "format": "binary"
              }
            },
            "image/png": {
              "schema": {
                "type": "string",
                "format": "binary"
              }
            }
          },
          "required": true
        },
        "responses" : {
          "200" : {
            "description" : "Default response"
          }
        }
      }
    },
    "/user/{userid}": {
      "get": {
        "summary": "Get specific user",
        "description": "Get a specific user with his/her id",
        "operationId": "getSpecificUser",
        "parameters": [ {
          "name": "userid",
          "in": "path",
          "required": true,
          "schema": {
            "type": "string"
          }
        } ],
        "responses": {
          "200": {
            "description": "Request successful",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/User"
                }
              },
              "application/xml": {
                "schema": {
                  "$ref": "#/components/schemas/User"
                }
              }
            }
          }
        }
      }
    },
    "/users/{my-path-param}": {
      "get": {
        "tags": ["user"],
        "summary": "Get users with myPathParam",
        "operationId": "getUsersWithMyPathParam",
        "parameters": [
          {
            "name": "my-path-param",
            "in": "path",
            "description": "My path param",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int32"
            }
          },
          {
            "name": "my-cookie",
            "in": "cookie",
            "description": "My cookie",
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "x-my-header",
            "in": "header",
            "description": "My header",
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "name",
            "in": "query",
            "description": "The name of the users you want to filter",
            "required": true,
            "deprecated": true,
            "allowEmptyValue": true,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "age",
            "in": "query",
            "schema": {
              "type": "integer",
              "format": "int32"
            }
          }
        ],
        "responses": $complexExampleUsersGetResponsesJson
      }
    },
    "/users2": {
      "get": {
        "tags": ["user"],
        "summary": "Get users2",
        "operationId": "getUsers2",
        "responses": $complexExampleUsersGetResponsesJson,
        "security": [
          { "http": ["myScope"] }
        ]
      }
    },
    "/form-data": {
      "put": {
        "summary": "Put formData",
        "operationId": "putFormData",
        "requestBody": {
          "content": {
            "application/x-www-form-urlencoded": {
              "schema": {
                "required" : [ "name" ],
                "type": "object",
                "properties": {
                  "name": {
                    "type": "string"
                  },
                  "age": {
                    "type": "integer",
                    "format": "int32"
                  }
                }
              }
            }
          }
        },
        "responses": {
          "200": {
            "description": "OK"
          }
        }
      }
    },
    "/form-data-schema": {
      "put": {
        "summary": "Put formDataSchema",
        "operationId": "putFormDataSchema",
        "requestBody": {
          "content": {
            "application/x-www-form-urlencoded": {
              "schema": {
                "$ref": "#/components/schemas/Address"
              }
            }
          }
        },
        "responses": {
          "200": {
            "description": "OK"
          }
        }
      }
    },
    "/form-data-schema-multipart": {
      "put": {
        "summary": "Put formDataSchemaMultipart",
        "operationId": "putFormDataSchemaMultipart",
        "requestBody": {
          "content": {
            "multipart/form-data": {
              "schema": {
                "$ref": "#/components/schemas/Address"
              }
            }
          }
        },
        "responses": {
          "200": {
            "description": "OK"
          }
        }
      }
    },
    "/string": {
      "get": {
        "summary": "Get string",
        "operationId": "getString",
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "text/plain": {
                "schema": {
                  "type": "string"
                }
              }
            }
          }
        }
      }
    },
    "/homepage": {
      "get": {
        "summary": "Get homepage",
        "operationId": "getHomepage",
        "responses": {
          "200": {
            "description": "My Homepage",
            "content": {
              "text/html": {
                "schema": {
                  "type": "string"
                }
              }
            }
          }
        }
      }
    },
    "/upload": {
      "get": {
        "summary": "Get upload",
        "operationId": "getUpload",
        "requestBody": {
          "description": "MyFile",
          "content": {
            "multipart/form-data": {
              "schema": {
                "type": "object",
                "properties": {
                  "file": {
                    "type": "string",
                    "format": "binary"
                  }
                }
              }
            }
          },
          "required": true
        },
        "responses" : {
          "200" : {
            "description" : "Default response"
          }
        }
      }
    },
    "/uploads": {
      "get": {
        "summary": "Get uploads",
        "operationId": "getUploads",
        "requestBody": {
          "description": "MyFiles",
          "content": {
            "multipart/form-data": {
              "schema": {
                "type": "object",
                "properties": {
                  "files": {
                    "type": "array",
                    "items": {
                      "type": "string",
                      "format": "binary"
                    }
                  }
                }
              }
            }
          },
          "required": true
        },
        "responses" : {
          "200" : {
            "description" : "Default response"
          }
        }
      }
    },
    "/upload-with-form-data": {
      "get": {
        "summary": "Get uploadWithFormData",
        "operationId": "getUploadWithFormData",
        "requestBody": {
          "description": "MyFile",
          "content": {
            "multipart/form-data": {
              "schema": {
                "type": "object",
                "properties": {
                  "file": {
                    "type": "string",
                    "format": "binary"
                  },
                  "title": {
                    "type": "string"
                  }
                }
              }
            }
          },
          "required": true
        },
        "responses" : {
          "200" : {
            "description" : "Default response"
          }
        }
      }
    },
    "/resources/*": {
      "get": {
        "summary": "Get resources with wildcard",
        "operationId": "getResourcesWithWildcard",
        "responses": {
          "200": {
            "description": "OK"
          }
        }
      }
    }
  },
  "components": {
    "schemas": {
      "Address": $addressOpenApiSchema,
      "User": $userOpenApiSchema
    },
    "securitySchemes" : {
      "http" : {
        "type" : "http",
        "scheme" : "basic"
      },
      "apiKey" : {
        "type" : "apiKey",
        "name" : "token",
        "in" : "cookie"
      }
    }
  }
}
""".formatJson()

@Language("JSON")
val crudExampleJson = """
{
  "openapi": "3.0.1",
  "info": {
    "title": "Example",
    "version": "1.0.0"
  },
  "paths": {
    "/users/{user-id}": {
      "get": {
        "summary": "Get users with userId",
        "operationId": "getUsersWithUserId",
        "parameters": [
          {
            "name": "user-id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/User"
                }
              }
            }
          }
        }
      },
      "delete": {
        "summary": "Delete users with userId",
        "operationId": "deleteUsersWithUserId",
        "parameters": [
          {
            "name": "user-id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses" : {
          "200" : {
            "description" : "Default response"
          }
        }
      },
      "patch": {
        "summary": "Patch users with userId",
        "operationId": "patchUsersWithUserId",
        "parameters": [
          {
            "name": "user-id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses" : {
          "200" : {
            "description" : "Default response"
          }
        }
      }
    },
    "/users": {
      "get": {
        "summary": "Get users",
        "operationId": "getUsers",
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "application/json": {
                "schema": {
                  "type": "array",
                  "items": {
                    "$ref": "#/components/schemas/User"
                  }
                }
              }
            }
          }
        }
      },
      "post": {
        "summary": "Post users",
        "operationId": "postUsers",
        "responses" : {
          "200" : {
            "description" : "Default response"
          }
        }
      }
    }
  },
  "components": {
    "schemas": {
      "Address": $addressOpenApiSchema,
      "User": $userOpenApiSchema
    }
  }
}
""".formatJson()

@Language("JSON")
val defaultOperationExampleJson = """
{
  "openapi": "3.0.1",
  "info": {
    "title": "Example",
    "version": "1.0.0"
  },
  "paths": {
    "/route1": {
      "get": {
        "summary": "Get route1",
        "operationId": "getRoute1",
        "responses": {
          "500": {
            "description": "Server Error",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/MyError"
                }
              }
            }
          },
          "200": {
            "description": "OK",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/User"
                }
              }
            }
          }
        }
      }
    },
    "/route2": {
      "get": {
        "summary": "Get route2",
        "operationId": "getRoute2",
        "responses": {
          "500": {
            "description": "Server Error",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/MyError"
                }
              }
            }
          }
        }
      }
    }
  },
  "components": {
    "schemas": {
      "MyError" : {
        "required" : [ "message" ],
        "type" : "object",
        "properties" : {
          "message" : {
            "type" : "string"
          }
        }
      },
      "Address": $addressOpenApiSchema,
      "User": $userOpenApiSchema
    }
  }
}
""".formatJson()

val overrideJson = """
{
  "openapi" : "3.0.1",
  "info" : {
    "title" : "Override Example",
    "version" : "1.0.0"
  },
  "paths" : {
    "/user" : {
      "get" : {
        "summary" : "Get user",
        "description" : "post description overwritten",
        "operationId" : "getUser",
        "responses" : {
          "200" : {
            "description" : "OK",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/User"
                }
              }
            }
          }
        }
      },
      "post" : {
        "summary" : "Post user",
        "description" : "get description overwritten",
        "operationId" : "postUser",
        "responses" : {
          "200" : {
            "description" : "Default response"
          }
        }
      }
    },
    "/unimplemented" : {
      "get" : {
        "summary" : "Get unimplemented",
        "operationId" : "getUnimplemented",
        "responses" : {
          "200" : {
            "description" : "Default response"
          }
        }
      }
    }
  },
  "components" : {
    "schemas" : {
      "Address" : {
        "required" : [ "number", "street" ],
        "type" : "object",
        "properties" : {
          "street" : {
            "type" : "string"
          },
          "number" : {
            "type" : "integer",
            "format" : "int32"
          }
        }
      },
      "User" : {
        "required" : [ "name" ],
        "type" : "object",
        "properties" : {
          "name" : {
            "type" : "string"
          },
          "address" : {
            "$ref" : "#/components/schemas/Address"
          }
        }
      }
    }
  }
}
""".formatJson()

@Language("json")
val userWithIdJsonExpected = """
{
	"get": {
		"summary": "Get specific user",
		"description": "Get a specific user with his/her id",
		"operationId": "getSpecificUser",
		"responses": {
			"200": {
				"description": "Request successful",
				"content": {
					"application/xml": {
						"schema": {
							"$ref": "#/components/schemas/User"
						}
					},
					"application/json": {
						"schema": {
							"$ref": "#/components/schemas/User"
						}
					}
				}
			}
		},
		"parameters": [
			{
				"schema": {
					"type": "string"
				},
				"in": "path",
				"name": "userid",
				"required": true
			}
		]
	}
}
""".trimIndent()

@Language("json")
val userJsonExpected = """
{
	"get": {
		"summary": "Get current user",
		"deprecated": true,
		"description": "Get a specific user",
		"operationId": "getCurrentUser",
		"responses": {
			"200": {
				"description": "Request successful",
				"content": {
					"application/xml": {
						"schema": {
							"$ref": "#/components/schemas/User"
						}
					},
					"application/json": {
						"schema": {
							"$ref": "#/components/schemas/User"
						}
					}
				}
			}
		},
		"tags": [
			"user"
		]
	},
	"description": "Some additional information for the /user endpoint"
}
""".trimIndent()


@Language("json")
val composedExample = """
{
  "openapi" : "3.0.1",
  "info" : {
    "title" : "Example",
    "version" : "1.0.0"
  },
  "paths" : {
    "/composed-body/any-of" : {
      "get" : {
        "summary" : "Get body with any of objects",
        "operationId" : "composedBodyAnyOf",
        "requestBody" : {
          "content" : {
            "application/json" : {
              "schema" : {
                "anyOf" : [ {
                  "$ref" : "#/components/schemas/Address"
                }, {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/User"
                  }
                } ]
              }
            }
          }
        },
        "responses" : {
          "200" : {
            "description" : "Default response"
          }
        }
      }
    },
    "/composed-body/one-of" : {
      "get" : {
        "summary" : "Get body with one of objects",
        "operationId" : "composedBodyOneOf",
        "requestBody" : {
          "content" : {
            "application/json" : {
              "schema" : {
                "oneOf" : [ {
                  "$ref" : "#/components/schemas/Address"
                }, {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/User"
                  }
                } ]
              }
            }
          }
        },
        "responses" : {
          "200" : {
            "description" : "Default response"
          }
        }
      }
    },
    "/composed-response/one-of" : {
      "get" : {
        "summary" : "Get with one of responses",
        "operationId" : "composedResponseOneOf",
        "responses" : {
          "200" : {
            "description" : "OK",
            "content" : {
              "application/json" : {
                "schema" : {
                  "oneOf" : [ {
                    "$ref" : "#/components/schemas/Address"
                  }, {
                    "$ref" : "#/components/schemas/User"
                  } ]
                }
              },
              "application/xml" : {
                "schema" : {
                  "$ref" : "#/components/schemas/User"
                }
              }
            }
          }
        }
      }
    }
  },
  "components" : {
    "schemas" : {
      "Address" : {
        "required" : [ "number", "street" ],
        "type" : "object",
        "properties" : {
          "street" : {
            "type" : "string"
          },
          "number" : {
            "type" : "integer",
            "format" : "int32"
          }
        }
      },
      "User" : {
        "required" : [ "name" ],
        "type" : "object",
        "properties" : {
          "name" : {
            "type" : "string"
          },
          "address" : {
            "$ref" : "#/components/schemas/Address"
          }
        }
      }
    }
  }
}
""".trimIndent()
