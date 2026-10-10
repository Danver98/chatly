import { useEffect, useState } from "react";
import { useAuthStore } from "../../../stores/authStore";
import User from "../../../models/User";
import UserService from "../../../services/UserService";
import { useFilePicker } from 'use-file-picker';
import './Profile.css';
import { Avatar, Box, Button, List, ListItem } from "@mui/material";


export default function Profile() {
    const currentLoggedUser = useAuthStore(state => state.currentLoggedUser);
    const [user, setUser] = useState<User | null>();
    const [readOnly, setReadOnly] = useState(true);
    const [imageFile, setImageFile] = useState<File|null>(null);
    const { openFilePicker, filesContent, loading } = useFilePicker({
        accept: ['jpg', 'jpeg', 'png'],
        readAs: 'ArrayBuffer',
        multiple: false,
        onFilesSuccessfullySelected: async ({ plainFiles, filesContent }: {plainFiles: any, filesContent: any}) => {
            // this callback is called when there were no validation errors
            //setImageFile(filesContent[0]);
            setImageFile(plainFiles[0]);
            if (!user?.id) return;
            const url = await UserService.setAvatar(user?.id, plainFiles[0]);
            const userCopy = User.copy(user);
            userCopy.avatar = url
            setUser(userCopy);
          },
      });

    const ImagePickerComponent = ({readOnly}: {readOnly: boolean | null}) => {
        return (
            <Button
                size="small"
                variant="contained"
                disabled={readOnly || false}
                onClick={() => {openFilePicker()}}
                >
                Set avatar
            </Button>
        )
    }

    const saveHandler = async () => {
        if (!readOnly) {
            // Save user
            await UserService.update(user);
            useAuthStore.getState().setCurrentLoggedUser(user ?? null);
        }
        setReadOnly(!readOnly);
    }

    useEffect(() => {
        const fetchUserData = async (id: number | string) => {
            const u = await UserService.get(id);
            setUser(u);
        };
        if (currentLoggedUser?.id) {
            fetchUserData(currentLoggedUser.id);
        }
    }, []);

    return (
        <div className="profile">
            <div className="profile__header">
                <Button
                    size="small"
                    variant="contained"
                    onClick={() => { saveHandler() }}>{
                        readOnly ? 'Change' : 'Save'
                    }
                </Button>
            </div>
            <div className="profile__content">
                <Box>
                    <Avatar
                        alt="User's avatar image"
                        src={user?.avatar}
                        sx={{
                            width: 100,
                            height: 100,
                            marginBottom: 1
                        }}
                    />
                    <ImagePickerComponent readOnly={readOnly}/>
                    {/* <ImagePickerComponentNative readOnly={readOnly} handleChange={fileChanged}/> */}
                    <List>
                        <ListItem>

                        </ListItem>
                        <ListItem>

                        </ListItem>
                    </List>
                </Box>
            </div>
        </div>
    )
}