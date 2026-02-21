# Booky Backend

Booky is a comprehensive backend service for a social book management and reading platform. It provides a robust set of APIs to power a rich user experience, from personal library management to interactive community features. The application is built with Java 17 and Spring Boot, following Clean Architecture principles for a modular and maintainable codebase.

## Core Features

-   **User & Profile Management**: Secure user authentication (sign-up/sign-in) with JWT, profile updates, and social following system.
-   **Personal Library**: Add books via ISBN search (powered by Google Books), track reading status (`WISHLIST`, `READING`, `TO_READ`, `READ`), and manage a list of favorite books.
-   **Book Exchange System**: A complete workflow for users to exchange physical books, including creating requests, making counter-offers, accepting/rejecting exchanges, and rating completed transactions.
-   **Community & Social Feed**: Create and join communities, create posts within communities or a general feed, comment on posts, and like posts. Users have a personalized feed based on people they follow.
-   **Reading Clubs**: Form reading clubs within communities for specific books. Includes features for managing meetings and membership.
-   **Live Meetings with LiveKit**: Integrated real-time video/audio meetings for reading clubs, powered by LiveKit. The system manages room creation and token generation for participants and moderators.
-   **AI-Powered 360° Scene Generation**: A unique feature that uses the OpenAI API (GPT-4o and DALL-E 3) to generate immersive 360° equirectangular images from text fragments of a book, enhancing virtual reading club experiences.
-   **Gamification Engine**: An engaging system that awards users points, achievements, and levels for various activities like reading books, completing exchanges, creating posts, and joining communities.
-   **Advanced User Search**: Find other users by username, books they offer for exchange (ordered by geographic proximity), or within a specific geographic location.

## Architecture

The project is structured following **Clean Architecture** and **Ports and Adapters** (Hexagonal Architecture) principles to ensure a clear separation of concerns.

-   `src/main/java/com/uade/bookybe/core`: Represents the core domain logic, containing business models, use cases (services), and ports (interfaces). It has no dependencies on external frameworks like Spring or specifics of the database.
-   `src/main/java/com/uade/bookybe/infraestructure`: Contains the implementations of the ports defined in the core. This includes database repositories (using Spring Data JPA) and adapters for external services like Google Books, Cloudinary, and AWS S3.
-   `src/main/java/com/uade/bookybe/router`: The entry point to the application, containing Spring MVC controllers that handle HTTP requests. This layer is responsible for exposing the API, handling DTOs, and invoking the core use cases.

### Key Technologies & Integrations
-   **Framework**: Spring Boot 3
-   **Language**: Java 17
-   **Database**: PostgreSQL
-   **Authentication**: Spring Security with JWT
-   **Data Access**: Spring Data JPA / Hibernate
-   **Mapping**: MapStruct for mapping between DTOs, Models, and Entities.
-   **Image Storage**: Supports both **Cloudinary** and **AWS S3** as configurable storage strategies.
-   **Book Data**: **Google Books API** for fetching book metadata.
-   **Real-time Video**: **LiveKit** for virtual reading club meetings.
-   **AI Generation**: **OpenAI API** for generating 360° scene images.

## API Documentation
The API is documented using Swagger/OpenAPI. Once the application is running, you can access the interactive documentation at:

`http://localhost:8080/swagger-ui.html`

## Getting Started

### Prerequisites
-   Java 17
-   Maven 3.9+
-   Docker (for running with Docker)

### Configuration
The application is configured via `src/main/resources/application.yml`. Key environment variables can be set to override default values, especially for production:

-   `DATABASE_URL`: Full PostgreSQL connection URL.
-   `DATABASE_USERNAME`: Database username.
-   `DATABASE_PASSWORD`: Database password.
-   `SECURITY_ENABLED`: Set to `true` for production to enable JWT security.
-   `JWT_SECRET`: A strong secret key for signing JWTs.
-   `IMAGE_STORAGE_STRATEGY`: `cloudinary` or `s3`.
-   `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`: Credentials for Cloudinary.
-   `AWS_S3_ACCESS_KEY`, `AWS_S3_SECRET_KEY`, `AWS_S3_REGION`, `AWS_S3_BUCKET`: Credentials for AWS S3.
-   `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET`, `LIVEKIT_WS_URL`: Credentials for LiveKit.
-   `OPENAI_API_KEY`: API key for OpenAI.

### Running Locally
1.  Clone the repository:
    ```bash
    git clone https://github.com/felipecosta03/booky-be.git
    cd booky-be
    ```

2.  Run the application using the Maven wrapper:
    ```bash
    ./mvnw spring-boot:run
    ```
The application will be available at `http://localhost:8080`.

### Running with Docker
1.  Build the Docker image:
    ```bash
    docker build -t booky-be .
    ```

2.  Run the container, providing the necessary environment variables:
    ```bash
    docker run -p 8080:8080 \
      -e DATABASE_URL="your_db_url" \
      -e JWT_SECRET="your_jwt_secret" \
      -e OPENAI_API_KEY="your_openai_key" \
      --name booky-backend booky-be
    ```

## Deployment
The repository is configured for deployment on [Fly.io](https://fly.io/) using the `fly.toml` file. It is set up to run a minimum of two machines for high availability.