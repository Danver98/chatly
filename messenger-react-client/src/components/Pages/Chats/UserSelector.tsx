import { Button, Checkbox, Dialog, InputAdornment, ListItemAvatar, TextField } from "@mui/material";
import "./ChatRoom.css";
import SearchIcon from '@mui/icons-material/Search';
import Box from '@mui/material/Box';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import User from "../../../models/User";
import { useCallback, useEffect, useMemo, useState } from "react";
import UserService, { UserFilter, UserRequestDTO } from "../../../services/UserService";
import { Avatar } from "react-lorem-ipsum";
import { ID } from "../../../util/Types";

const SearchBar = ({ onChange }: { onChange: (value: string) => any }) => {
    return (
        <TextField
            id="standard-basic"
            label="Enter user credentials"
            variant="standard"
            fullWidth
            margin="normal"
            slotProps={{
                input: {
                    endAdornment: (
                        <InputAdornment position="end">
                            <SearchIcon />
                        </InputAdornment>
                    ),
                }
            }}
            onChange={(e: any) => {
                onChange(e.target.value)
            }}
        />
    )
}

const UserList = ({ users, checked, handleToggle }: { users: User[], checked: number[], handleToggle: (position: number, userId: ID) => any }) => {
    if (users == null || users.length === 0) {
        return (
            <>
                No users found!
            </>
        )
    }
    return (
        <Box >
            <List>
                {
                    users.map((user, index) =>
                        <ListItem
                            key={user.id}
                            secondaryAction={
                                <Checkbox
                                    checked={checked.includes(index)}
                                    onChange={(_event) => handleToggle(index, user.id)}
                                />
                            }
                        >
                            <ListItemAvatar >
                                <Avatar src={user.avatar} />
                            </ListItemAvatar>
                            <ListItemText
                                primary={user.name + ' ' + user.surname}
                            >
                            </ListItemText>
                        </ListItem>
                    )
                }
            </List>
        </Box>
    )
}

const UserSelector = ({ isOpen = false, submitText = 'Select', cancelText = 'Cancel', filter = {}, onResult, onCancel}: {
    isOpen: boolean
    submitText?: string,
    cancelText?: string,
    filter?: UserFilter,
    onResult: (elements: any[], params?: any | null) => any,
    onCancel?: () => any
}) => {
    const [open, setOpen] = useState(isOpen);
    const [users, setUsers] = useState<User[]>([]);
    const [search, setSearch] = useState("");
    const [checked, setChecked] = useState<number[]>([]);
    const [selectedUsers, setSelectedUsers] = useState<ID[]>([]);

    const handleToggle = (position: number, userId: ID) => {
        const currentIndex = checked.indexOf(position);
        const newChecked = [...checked];
        let newSelectedUsers = [...selectedUsers];

        if (currentIndex === -1) {
            newChecked.push(position);
            newSelectedUsers.push(userId);
        } else {
            newChecked.splice(currentIndex, 1);
            newSelectedUsers = newSelectedUsers.filter((id) => id !== userId);
        }

        setChecked(newChecked);
        setSelectedUsers(newSelectedUsers);
    };

    const handleClickClose = useCallback(() => {
        setOpen(false)
    }, []);

    const cancelSelection = useCallback(() => {
        setChecked([]);
        setSelectedUsers([]);
        handleClickClose();
        onCancel?.();
    }, [handleClickClose, onCancel]);

    const submitSelection = useCallback(() => {
        setChecked([]);
        setSelectedUsers([]);
        handleClickClose();
        onResult(selectedUsers);
    }, [handleClickClose, onResult, selectedUsers]);

    const abortController = useMemo(() => {
        return new AbortController();
    }, []);

    useEffect(() => {
        setOpen(isOpen);
    }, [isOpen]);

    useEffect(() => {
        const fetchUsers = async () => {
            if (!open) return;
            if (search) {
                // In case of search is present start requesting from 3 symbols-lenght string
                if (search.length > 2) {
                    const data = await UserService.list(dto, abortController);
                    setUsers(data);
                }
            } else {
                const data = await UserService.list(dto, abortController);
                // Check if answer contains data
                setUsers(data);
            }
        }
        // If we've already started a new request, cancel it
        const dto: UserRequestDTO = {
            filter: {
                ...filter,
                search: search
            }
        }

        fetchUsers();

        return () => {
            //abortController.abort();
        };

    }, [open, search, abortController, filter])

    return (
        <div className="">
            <Dialog
                open={open}
                onClose={() => handleClickClose()}
            >
                <div className="participant-selection-popup">
                    <div className="participant-selection-popup-content-wrapper">
                        <div className="participant-selection-popup__SearchBar">
                            <SearchBar
                                onChange={setSearch}
                            />
                        </div>
                        <div className="participant-selection-popup__UserList">
                            <UserList
                                users={users}
                                checked={checked}
                                handleToggle={handleToggle}
                            />
                        </div>
                        <div className="participant-selection-popup__Confirmation">
                            <div className="">
                                <Button
                                    variant="contained"
                                    onClick={() => cancelSelection()}
                                >
                                    {cancelText}
                                </Button>
                                <Button
                                    variant="contained"
                                    disabled={checked.length === 0}
                                    onClick={() => submitSelection() }
                                >
                                    {submitText}
                                </Button>
                            </div>
                        </div>
                    </div>
                </div>
            </Dialog>
        </div>
    )
};
export default UserSelector;