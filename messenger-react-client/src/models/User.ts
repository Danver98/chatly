interface User {
    id: number | string;
    name?: string;
    surname?: string;
    login?: string;
    avatar?: string;
    password?: string;
}

class User implements User {
    id: number | string;
    name?: string;
    surname?: string;
    login?: string;
    avatar?: string;
    password?: string;

    public static copy(user: User) {
        return new User(
            user.id,
            user.name,
            user.surname,
            user.login,
            user.avatar,
            user.password
        );
    };

    constructor(id: number | string, name?: string, surname?: string, login?: string, avatar?: string, password?: string) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.login = login;
        this.avatar = avatar;
        this.password = password;
    }
}

export default User;